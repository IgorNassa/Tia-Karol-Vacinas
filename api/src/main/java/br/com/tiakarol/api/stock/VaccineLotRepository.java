package br.com.tiakarol.api.stock;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface VaccineLotRepository extends JpaRepository<VaccineLot, UUID> {
    Optional<VaccineLot> findByVaccineNameIgnoreCaseAndLotCodeIgnoreCase(String vaccineName, String lotCode);
}
