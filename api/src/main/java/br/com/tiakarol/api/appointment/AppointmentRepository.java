package br.com.tiakarol.api.appointment;

import java.util.UUID;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    Page<Appointment> findByStatusAndReservationStatus(AppointmentStatus status,
                                                        ReservationStatus reservationStatus,
                                                        Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select appointment from Appointment appointment where appointment.id = :id")
    Optional<Appointment> findByIdForUpdate(@Param("id") UUID id);
}
