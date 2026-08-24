package br.com.tiakarol.api.stock;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface VaccineRepository extends JpaRepository<Vaccine, UUID> {
    @Query("select (count(v) > 0) from Vaccine v where lower(v.name) = lower(:name) "
            + "and lower(coalesce(v.vaccineType, '')) = lower(:vaccineType) "
            + "and lower(coalesce(v.manufacturer, '')) = lower(:manufacturer)")
    boolean existsByBusinessKey(@Param("name") String name, @Param("vaccineType") String vaccineType,
                                @Param("manufacturer") String manufacturer);
    Page<Vaccine> findByNameContainingIgnoreCase(String search, Pageable pageable);
}
