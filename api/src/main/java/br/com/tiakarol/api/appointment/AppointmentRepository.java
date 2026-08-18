package br.com.tiakarol.api.appointment;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    Page<Appointment> findByStatusAndReservationStatus(AppointmentStatus status,
                                                        ReservationStatus reservationStatus,
                                                        Pageable pageable);
}
