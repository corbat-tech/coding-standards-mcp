package com.example.orderprocessing.domain.repository;

import com.example.orderprocessing.domain.entity.InventoryItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<InventoryItem, String> {

    /**
     * Find inventory item with pessimistic lock for concurrent updates
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryItem i WHERE i.productId = :productId")
    Optional<InventoryItem> findByIdWithLock(@Param("productId") String productId);

    /**
     * Find all products with available stock
     */
    @Query("SELECT i FROM InventoryItem i WHERE (i.quantityAvailable - i.quantityReserved) > 0")
    List<InventoryItem> findAllWithAvailableStock();

    /**
     * Find products with low stock (below threshold)
     */
    @Query("SELECT i FROM InventoryItem i WHERE (i.quantityAvailable - i.quantityReserved) < :threshold")
    List<InventoryItem> findLowStockItems(@Param("threshold") int threshold);
}
