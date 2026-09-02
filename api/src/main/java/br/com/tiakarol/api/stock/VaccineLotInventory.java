package br.com.tiakarol.api.stock;

import br.com.tiakarol.api.audit.AuditService;
import br.com.tiakarol.api.security.CurrentUser;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class VaccineLotInventory {
    private final VaccineLotRepository lotRepository;
    private final StockMovementRepository movementRepository;
    private final CurrentUser currentUser;
    private final AuditService auditService;

    VaccineLotInventory(VaccineLotRepository lotRepository, StockMovementRepository movementRepository,
                        CurrentUser currentUser, AuditService auditService) {
        this.lotRepository = lotRepository;
        this.movementRepository = movementRepository;
        this.currentUser = currentUser;
        this.auditService = auditService;
    }

    public void reserve(UUID lotId, int quantity, UUID appointmentId) {
        VaccineLot lot = operationalLot(lotId);
        lot.getBalance().reserve(quantity);
        record(lot, StockMovementType.RESERVATION, quantity, "Reserva do agendamento " + appointmentId);
    }

    public void release(UUID lotId, int quantity, UUID appointmentId, String reason) {
        VaccineLot lot = lockedLot(lotId);
        lot.getBalance().releaseReservation(quantity);
        record(lot, StockMovementType.CANCELLATION, quantity,
                "Liberação do agendamento " + appointmentId + ": " + reason.trim());
    }

    public void apply(UUID lotId, int quantity, UUID appointmentId) {
        VaccineLot lot = operationalLot(lotId);
        lot.getBalance().applyReservation(quantity);
        record(lot, StockMovementType.APPLICATION, quantity, "Aplicação do agendamento " + appointmentId);
    }

    public void transferReservation(UUID sourceLotId, UUID targetLotId, int quantity, UUID appointmentId,
                                    String reason) {
        if (sourceLotId.equals(targetLotId)) return;
        UUID firstId = sourceLotId.compareTo(targetLotId) < 0 ? sourceLotId : targetLotId;
        UUID secondId = sourceLotId.equals(firstId) ? targetLotId : sourceLotId;
        VaccineLot first = lockedLot(firstId);
        VaccineLot second = lockedLot(secondId);
        VaccineLot source = sourceLotId.equals(firstId) ? first : second;
        VaccineLot target = targetLotId.equals(firstId) ? first : second;
        ensureOperational(target);
        target.getBalance().reserve(quantity);
        source.getBalance().releaseReservation(quantity);
        record(source, StockMovementType.CANCELLATION, quantity,
                "Transferência do agendamento " + appointmentId + ": " + reason.trim());
        record(target, StockMovementType.RESERVATION, quantity,
                "Transferência do agendamento " + appointmentId + ": " + reason.trim());
    }

    private VaccineLot operationalLot(UUID lotId) {
        VaccineLot lot = lockedLot(lotId);
        ensureOperational(lot);
        return lot;
    }

    private void ensureOperational(VaccineLot lot) {
        if (!lot.getVaccine().isActive()) {
            throw new StockDomainException("Vacina inativa não pode ser reservada ou aplicada.");
        }
        if (!lot.isActive()) {
            throw new StockDomainException("Lote inativo não pode ser reservado ou aplicado.");
        }
        if (!lot.getExpirationDate().isAfter(LocalDate.now())) {
            throw new StockDomainException("Lote vencido não pode ser reservado ou aplicado.");
        }
    }

    private VaccineLot lockedLot(UUID lotId) {
        return lotRepository.findForUpdateById(lotId)
                .orElseThrow(() -> new StockDomainException("Lote não encontrado."));
    }

    private void record(VaccineLot lot, StockMovementType type, int quantity, String reason) {
        movementRepository.save(new StockMovement(lot, type, quantity, reason, currentUser.id()));
        StockBalance balance = lot.getBalance();
        auditService.log("VACCINE_LOT", lot.getId(), "STOCK_" + type.name(), null,
                Map.of("physicalQuantity", balance.getPhysicalQuantity(),
                        "reservedQuantity", balance.getReservedQuantity(),
                        "availableQuantity", balance.getAvailableQuantity()), reason);
    }
}
