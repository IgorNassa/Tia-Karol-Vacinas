package br.com.tiakarol.api.finance;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RecurringExpenseRepository extends JpaRepository<RecurringExpense, UUID> {
    Page<RecurringExpense> findByActive(boolean active, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select expense from RecurringExpense expense where expense.id = :id")
    Optional<RecurringExpense> findByIdForUpdate(@Param("id") UUID id);
}
