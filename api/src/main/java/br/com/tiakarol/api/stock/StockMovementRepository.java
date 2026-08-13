package br.com.tiakarol.api.stock;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {
}
