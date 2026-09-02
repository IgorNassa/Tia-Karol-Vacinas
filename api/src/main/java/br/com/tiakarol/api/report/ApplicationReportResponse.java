package br.com.tiakarol.api.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ApplicationReportResponse(LocalDate fromDate, LocalDate toDate, UUID patientId,
                                        long totalApplications, BigDecimal totalAmount,
                                        List<Item> applications) {
    public record Item(UUID appointmentId, UUID patientId, String patientName, UUID vaccineId,
                       String vaccineName, String lotCode, OffsetDateTime appliedAt,
                       String applicationLocation, BigDecimal finalAmount,
                       BigDecimal receivedAmount, String paymentStatus) { }
}
