package br.com.tiakarol.api.stock;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vaccine-lots")
class VaccineLotController {
    private final VaccineLotService service;

    VaccineLotController(VaccineLotService service) { this.service = service; }

    @PostMapping
    ResponseEntity<VaccineLotResponse> create(@Valid @RequestBody VaccineLotRequest request) {
        VaccineLotMutationResult result = service.createOrIncrease(request);
        if (!result.created()) {
            return ResponseEntity.ok(result.response());
        }
        return ResponseEntity.created(URI.create("/api/v1/vaccine-lots/" + result.response().id()))
                .body(result.response());
    }

    @GetMapping("/{id}")
    VaccineLotResponse get(@PathVariable UUID id) { return service.get(id); }

    @GetMapping
    Page<VaccineLotResponse> list(Pageable pageable) { return service.list(pageable); }

    @PostMapping("/{id}/stock-movements")
    VaccineLotResponse move(@PathVariable UUID id, @Valid @RequestBody StockMovementRequest request) {
        return service.move(id, request);
    }
}
