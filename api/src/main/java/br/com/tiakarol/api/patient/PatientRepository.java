package br.com.tiakarol.api.patient;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

interface PatientRepository extends JpaRepository<Patient, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select patient from Patient patient where patient.id = :id")
    Optional<Patient> findForUpdateById(@Param("id") UUID id);
    boolean existsByIdentityTypeAndIdentityNumber(PatientIdentityType identityType, String identityNumber);
    Page<Patient> findByFullNameContainingIgnoreCase(String search, Pageable pageable);
}
