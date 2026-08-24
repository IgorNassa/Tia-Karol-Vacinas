package br.com.tiakarol.api.stock;

import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface VaccineLotRepository extends JpaRepository<VaccineLot, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select lot from VaccineLot lot join fetch lot.balance where lot.vaccine.id = :vaccineId and lower(lot.lotCode) = lower(:lotCode)")
    Optional<VaccineLot> findForUpdateByVaccineIdAndLotCode(@Param("vaccineId") UUID vaccineId,
                                                            @Param("lotCode") String lotCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select lot from VaccineLot lot join fetch lot.balance where lot.id = :id")
    Optional<VaccineLot> findForUpdateById(@Param("id") UUID id);
}
