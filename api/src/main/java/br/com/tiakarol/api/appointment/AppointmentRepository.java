package br.com.tiakarol.api.appointment;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface AppointmentRepository extends JpaRepository<Appointment, UUID>, JpaSpecificationExecutor<Appointment> {
    long countByPatientId(UUID patientId);
    long countByPatientIdAndStatus(UUID patientId, AppointmentStatus status);
    List<Appointment> findTop5ByPatientIdOrderByScheduledAtDesc(UUID patientId);
    Page<Appointment> findByStatusAndReservationStatus(AppointmentStatus status,
                                                        ReservationStatus reservationStatus,
                                                        Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select appointment from Appointment appointment where appointment.id = :id")
    Optional<Appointment> findByIdForUpdate(@Param("id") UUID id);
}
