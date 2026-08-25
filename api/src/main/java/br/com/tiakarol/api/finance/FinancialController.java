package br.com.tiakarol.api.finance;

import br.com.tiakarol.api.appointment.ReasonRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/financial")
class FinancialController {
    private final FinancialService service;

    FinancialController(FinancialService service) {
        this.service = service;
    }

    @PostMapping("/entries")
    ResponseEntity<FinancialEntryResponse> createEntry(@Valid @RequestBody FinancialEntryRequest request) {
        FinancialEntryResponse response = service.createEntry(request);
        return ResponseEntity.created(URI.create("/api/v1/financial/entries/" + response.id())).body(response);
    }

    @GetMapping("/entries")
    Page<FinancialEntryResponse> listEntries(
            @RequestParam(required = false) FinancialEntryType type,
            @RequestParam(required = false) FinancialCategory category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Boolean active, Pageable pageable) {
        return service.listEntries(type, category, fromDate, toDate, active, pageable);
    }

    @PostMapping("/entries/{id}/void")
    FinancialEntryResponse voidEntry(@PathVariable UUID id, @Valid @RequestBody ReasonRequest request) {
        return service.voidEntry(id, request.reason());
    }

    @PostMapping("/recurring-expenses")
    ResponseEntity<RecurringExpenseResponse> createRecurring(@Valid @RequestBody RecurringExpenseRequest request) {
        RecurringExpenseResponse response = service.createRecurring(request);
        return ResponseEntity.created(URI.create("/api/v1/financial/recurring-expenses/" + response.id()))
                .body(response);
    }

    @GetMapping("/recurring-expenses")
    Page<RecurringExpenseResponse> listRecurring(@RequestParam(required = false) Boolean active, Pageable pageable) {
        return service.listRecurring(active, pageable);
    }

    @PutMapping("/recurring-expenses/{id}")
    RecurringExpenseResponse updateRecurring(@PathVariable UUID id,
                                             @Valid @RequestBody RecurringExpenseRequest request) {
        return service.updateRecurring(id, request);
    }

    @PatchMapping("/recurring-expenses/{id}/inactivation")
    RecurringExpenseResponse inactivateRecurring(@PathVariable UUID id) {
        return service.inactivateRecurring(id);
    }

    @PostMapping("/recurring-expenses/{id}/payments")
    FinancialEntryResponse payRecurring(@PathVariable UUID id,
                                        @Valid @RequestBody RecurringExpensePaymentRequest request) {
        return service.payRecurring(id, request);
    }
}
