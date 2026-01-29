package com.example.orderprocessing.controller;

import com.example.orderprocessing.domain.entity.InventoryItem;
import com.example.orderprocessing.service.InventoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * REST controller for inventory operations.
 */
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    public ResponseEntity<InventoryResponse> addInventory(@Valid @RequestBody AddInventoryRequest request) {
        InventoryItem item = inventoryService.addOrUpdateInventory(
                request.productId(),
                request.productName(),
                request.quantity()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(InventoryResponse.from(item));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<InventoryResponse> getInventory(@PathVariable String productId) {
        return inventoryService.getInventory(productId)
                .map(item -> ResponseEntity.ok(InventoryResponse.from(item)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<InventoryResponse>> getAllInventory() {
        List<InventoryResponse> inventory = inventoryService.getAllInventory().stream()
                .map(InventoryResponse::from)
                .toList();
        return ResponseEntity.ok(inventory);
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<InventoryResponse>> getLowStockItems(
            @RequestParam(defaultValue = "10") int threshold) {
        List<InventoryResponse> items = inventoryService.getLowStockItems(threshold).stream()
                .map(InventoryResponse::from)
                .toList();
        return ResponseEntity.ok(items);
    }

    // Request/Response DTOs

    public record AddInventoryRequest(
            @NotBlank String productId,
            @NotBlank String productName,
            @PositiveOrZero int quantity
    ) {}

    public record InventoryResponse(
            String productId,
            String productName,
            int quantityAvailable,
            int quantityReserved,
            int effectiveAvailable,
            Instant updatedAt
    ) {
        public static InventoryResponse from(InventoryItem item) {
            return new InventoryResponse(
                    item.getProductId(),
                    item.getProductName(),
                    item.getQuantityAvailable(),
                    item.getQuantityReserved(),
                    item.getEffectiveAvailable(),
                    item.getUpdatedAt()
            );
        }
    }
}
