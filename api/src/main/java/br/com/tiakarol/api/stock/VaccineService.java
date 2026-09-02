package br.com.tiakarol.api.stock;

import br.com.tiakarol.api.audit.AuditService;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class VaccineService {
    private final VaccineRepository repository;
    private final AuditService auditService;

    VaccineService(VaccineRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional
    VaccineResponse create(VaccineRequest request) {
        ensureUnique(request, null);
        Vaccine vaccine = repository.save(new Vaccine(request));
        VaccineResponse response = toResponse(vaccine);
        auditService.log("VACCINE", vaccine.getId(), "VACCINE_CREATED", null, response, null);
        return response;
    }

    @Transactional(readOnly = true)
    VaccineResponse get(UUID id) { return toResponse(find(id)); }

    @Transactional(readOnly = true)
    Page<VaccineResponse> list(String search, Pageable pageable) {
        return (search == null || search.isBlank() ? repository.findAll(pageable)
                : repository.findByNameContainingIgnoreCase(search.trim(), pageable)).map(this::toResponse);
    }

    @Transactional
    VaccineResponse update(UUID id, VaccineRequest request) {
        Vaccine vaccine = find(id);
        VaccineResponse before = toResponse(vaccine);
        ensureUnique(request, vaccine);
        vaccine.update(request);
        VaccineResponse response = toResponse(vaccine);
        auditService.log("VACCINE", id, "VACCINE_UPDATED", before, response, null);
        return response;
    }

    @Transactional
    VaccineResponse inactivate(UUID id) {
        Vaccine vaccine = find(id);
        VaccineResponse before = toResponse(vaccine);
        vaccine.inactivate();
        VaccineResponse response = toResponse(vaccine);
        auditService.log("VACCINE", id, "VACCINE_INACTIVATED", before, response, null);
        return response;
    }

    Vaccine findActive(UUID id) {
        Vaccine vaccine = find(id);
        if (!vaccine.isActive()) throw new StockDomainException("Vacina inativa não pode receber novos lotes.");
        return vaccine;
    }

    private Vaccine find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new StockDomainException("Vacina não encontrada."));
    }

    private void ensureUnique(VaccineRequest request, Vaccine current) {
        String type = request.vaccineType() == null ? "" : request.vaccineType().trim();
        String manufacturer = request.manufacturer() == null ? "" : request.manufacturer().trim();
        boolean unchanged = current != null && current.getName().equalsIgnoreCase(request.name().trim())
                && nullSafe(current.getVaccineType()).equalsIgnoreCase(type)
                && nullSafe(current.getManufacturer()).equalsIgnoreCase(manufacturer);
        if (!unchanged && repository.existsByBusinessKey(
                request.name().trim(), type, manufacturer)) {
            throw new StockDomainException("Já existe vacina com o mesmo nome, tipo e fabricante.");
        }
    }

    private String nullSafe(String value) { return value == null ? "" : value; }
    private VaccineResponse toResponse(Vaccine item) {
        return new VaccineResponse(item.getId(), item.getName(), item.getVaccineType(), item.getManufacturer(), item.isActive());
    }
}
