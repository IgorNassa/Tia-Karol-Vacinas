package br.com.tiakarol.api.stock;

import br.com.tiakarol.api.audit.AuditService;
import br.com.tiakarol.api.security.CurrentUser;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class VaccineLotService {
    private final VaccineLotRepository lotRepository;
    private final StockMovementRepository movementRepository;
    private final CurrentUser currentUser;
    private final AuditService auditService;
    private final VaccineService vaccineService;

    VaccineLotService(VaccineLotRepository lotRepository, StockMovementRepository movementRepository,
                      CurrentUser currentUser, AuditService auditService, VaccineService vaccineService) {
        this.lotRepository = lotRepository;
        this.movementRepository = movementRepository;
        this.currentUser = currentUser;
        this.auditService = auditService;
        this.vaccineService = vaccineService;
    }

    @Transactional
    VaccineLotMutationResult createOrIncrease(VaccineLotRequest request) {
        Vaccine vaccine = vaccineService.findActive(request.vaccineId());
        return lotRepository.findForUpdateByVaccineIdAndLotCode(request.vaccineId(), request.lotCode())
                .map(existing -> increaseExisting(existing, request))
                .orElseGet(() -> createNew(vaccine, request));
    }

    @Transactional(readOnly = true)
    VaccineLotResponse get(UUID id) {
        return toResponse(find(id));
    }

    @Transactional(readOnly = true)
    Page<VaccineLotResponse> list(Pageable pageable) {
        return lotRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional
    VaccineLotResponse move(UUID id, StockMovementRequest request) {
        VaccineLot lot = findForUpdate(id);
        if (request.type() == StockMovementType.ADJUSTMENT && (request.reason() == null || request.reason().isBlank())) {
            throw new StockDomainException("Ajuste manual exige motivo.");
        }
        switch (request.type()) {
            case ENTRY, RETURN -> {
                ensureOperational(lot);
                lot.getBalance().addPhysical(request.quantity());
            }
            case ADJUSTMENT, LOSS, EXPIRATION -> lot.getBalance().removePhysical(request.quantity());
            default -> throw new StockDomainException("Esse tipo de movimento é controlado pela agenda e não pode ser lançado manualmente.");
        }
        movementRepository.save(new StockMovement(lot, request.type(), request.quantity(), trimToNull(request.reason()),
                currentUser.id()));
        VaccineLotResponse response = toResponse(lot);
        auditService.log("VACCINE_LOT", lot.getId(), "STOCK_" + request.type().name(), null, response,
                trimToNull(request.reason()));
        return response;
    }

    private VaccineLotMutationResult createNew(Vaccine vaccine, VaccineLotRequest request) {
        VaccineLot lot = lotRepository.save(new VaccineLot(vaccine, request));
        movementRepository.save(new StockMovement(lot, StockMovementType.ENTRY, request.initialQuantity(),
                "Entrada inicial do lote.", currentUser.id()));
        VaccineLotResponse response = toResponse(lot);
        auditService.log("VACCINE_LOT", lot.getId(), "VACCINE_LOT_CREATED", null, response, null);
        return new VaccineLotMutationResult(response, true);
    }

    private VaccineLotMutationResult increaseExisting(VaccineLot existing, VaccineLotRequest request) {
        if (!existing.matches(request)) {
            throw new StockDomainException("O lote informado já existe com dados divergentes. Revise validade, fornecedor e preços.");
        }
        existing.getBalance().addPhysical(request.initialQuantity());
        movementRepository.save(new StockMovement(existing, StockMovementType.ENTRY, request.initialQuantity(),
                "Entrada adicional no lote existente.", currentUser.id()));
        VaccineLotResponse response = toResponse(existing);
        auditService.log("VACCINE_LOT", existing.getId(), "STOCK_ENTRY", null, response,
                "Entrada adicional no lote existente.");
        return new VaccineLotMutationResult(response, false);
    }

    private VaccineLot find(UUID id) {
        return lotRepository.findById(id).orElseThrow(() -> new StockDomainException("Lote não encontrado."));
    }

    private VaccineLot findForUpdate(UUID id) {
        return lotRepository.findForUpdateById(id)
                .orElseThrow(() -> new StockDomainException("Lote não encontrado."));
    }

    private void ensureOperational(VaccineLot lot) {
        if (!lot.getVaccine().isActive()) {
            throw new StockDomainException("Vacina inativa não pode receber operação.");
        }
        if (!lot.isActive()) {
            throw new StockDomainException("Lote inativo não pode receber operação.");
        }
        if (!lot.getExpirationDate().isAfter(LocalDate.now())) {
            throw new StockDomainException("Lote vencido não pode receber operação.");
        }
    }

    private VaccineLotResponse toResponse(VaccineLot lot) {
        StockBalance balance = lot.getBalance();
        Vaccine vaccine = lot.getVaccine();
        return new VaccineLotResponse(lot.getId(), vaccine.getId(), vaccine.getName(), vaccine.getVaccineType(), lot.getLotCode(),
                lot.getExpirationDate(), vaccine.getManufacturer(), lot.getSupplier(), lot.getInvoiceNumber(),
                lot.getPurchasePrice(), lot.getSalePrice(), lot.getNotes(), lot.isActive(), balance.getPhysicalQuantity(),
                balance.getReservedQuantity(), balance.getAvailableQuantity());
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
