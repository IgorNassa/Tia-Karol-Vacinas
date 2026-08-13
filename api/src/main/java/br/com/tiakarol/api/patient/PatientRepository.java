package br.com.tiakarol.api.patient;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface PatientRepository extends JpaRepository<Patient, UUID> {
    boolean existsByIdentityTypeAndIdentityNumber(PatientIdentityType identityType, String identityNumber);
    Page<Patient> findByFullNameContainingIgnoreCase(String search, Pageable pageable);
}
