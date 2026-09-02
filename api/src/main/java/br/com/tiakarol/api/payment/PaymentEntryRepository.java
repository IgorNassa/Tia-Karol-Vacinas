package br.com.tiakarol.api.payment;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PaymentEntryRepository extends JpaRepository<PaymentEntry, UUID> {
    List<PaymentEntry> findByAppointmentIdAndActiveTrueOrderByCreatedAtAsc(UUID appointmentId);
    List<PaymentEntry> findByAppointmentIdOrderByCreatedAtAsc(UUID appointmentId);
    boolean existsByAppointmentIdAndActiveTrue(UUID appointmentId);
}
