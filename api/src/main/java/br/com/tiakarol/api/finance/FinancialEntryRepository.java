package br.com.tiakarol.api.finance;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface FinancialEntryRepository extends JpaRepository<FinancialEntry, UUID>,
        JpaSpecificationExecutor<FinancialEntry> {
    boolean existsByRecurringExpenseIdAndActiveTrueAndOccurredOnBetween(UUID recurringExpenseId,
                                                                        LocalDate fromDate, LocalDate toDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select entry from FinancialEntry entry where entry.id = :id")
    Optional<FinancialEntry> findByIdForUpdate(@Param("id") UUID id);
}
