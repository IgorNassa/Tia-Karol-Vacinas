package br.com.tiakarol.api.stock;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vaccines")
class VaccineController {
    private final VaccineService service;
    VaccineController(VaccineService service) { this.service = service; }

    @PostMapping
    ResponseEntity<VaccineResponse> create(@Valid @RequestBody VaccineRequest request) {
        VaccineResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/vaccines/" + response.id())).body(response);
    }
    @GetMapping("/{id}") VaccineResponse get(@PathVariable UUID id) { return service.get(id); }
    @GetMapping Page<VaccineResponse> list(@RequestParam(required = false) String search, Pageable pageable) {
        return service.list(search, pageable);
    }
    @PutMapping("/{id}") VaccineResponse update(@PathVariable UUID id, @Valid @RequestBody VaccineRequest request) {
        return service.update(id, request);
    }
    @PatchMapping("/{id}/inactivation") VaccineResponse inactivate(@PathVariable UUID id) {
        return service.inactivate(id);
    }
}
