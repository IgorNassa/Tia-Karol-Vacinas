package br.com.tiakarol.api.report;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
class ReportController {
    private final ReportService service;

    ReportController(ReportService service) {
        this.service = service;
    }

    @GetMapping("/financial/summary")
    FinancialSummaryResponse financialSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return service.financialSummary(fromDate, toDate);
    }

    @GetMapping("/financial/projections")
    FinancialProjectionResponse projections(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return service.projections(fromDate, toDate);
    }

    @GetMapping("/financial/cash-flow")
    CashFlowReportResponse cashFlow(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return service.cashFlow(fromDate, toDate);
    }

    @GetMapping("/applications")
    ApplicationReportResponse applications(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return service.applications(fromDate, toDate, null);
    }

    @GetMapping("/patients/{patientId}/applications")
    ApplicationReportResponse patientApplications(
            @PathVariable UUID patientId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return service.applications(fromDate, toDate, patientId);
    }

    @GetMapping("/stock")
    StockReportResponse stock() {
        return service.stock();
    }
}
