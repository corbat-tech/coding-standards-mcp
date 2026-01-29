package com.example.orderprocessing.integration;

import com.example.orderprocessing.config.KafkaConfig;
import com.example.orderprocessing.controller.InventoryController;
import com.example.orderprocessing.controller.OrderController;
import com.example.orderprocessing.domain.entity.Order;
import com.example.orderprocessing.domain.repository.InventoryRepository;
import com.example.orderprocessing.domain.repository.OrderRepository;
import com.example.orderprocessing.domain.repository.ProcessedEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration test for the complete order processing flow.
 * Tests the full cycle: REST API -> Order Service -> Kafka -> Inventory Service
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"},
        topics = {KafkaConfig.ORDER_CREATED_TOPIC, KafkaConfig.ORDER_CREATED_DLT}
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class EndToEndIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @BeforeEach
    void setUp() {
        processedEventRepository.deleteAll();
        orderRepository.deleteAll();
        inventoryRepository.deleteAll();
    }

    @Test
    @DisplayName("Complete order flow: create order -> publish event -> update inventory")
    void shouldCompleteOrderFlowEndToEnd() throws Exception {
        // Step 1: Set up inventory via REST API
        InventoryController.AddInventoryRequest inventoryRequest = new InventoryController.AddInventoryRequest(
                "LAPTOP-001",
                "Gaming Laptop",
                50
        );

        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inventoryRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value("LAPTOP-001"))
                .andExpect(jsonPath("$.quantityAvailable").value(50));

        // Step 2: Create order via REST API
        OrderController.CreateOrderRequest orderRequest = new OrderController.CreateOrderRequest(
                "CUST-001",
                List.of(
                        new OrderController.CreateOrderItemRequest(
                                "LAPTOP-001",
                                "Gaming Laptop",
                                2,
                                new BigDecimal("999.99")
                        )
                )
        );

        String orderResponse = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value("CUST-001"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(1999.98))
                .andReturn()
                .getResponse()
                .getContentAsString();

        OrderController.OrderResponse createdOrder = objectMapper.readValue(
                orderResponse, OrderController.OrderResponse.class);

        // Step 3: Wait for Kafka consumer to process the event and update inventory
        await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    // Verify inventory was updated
                    var inventory = inventoryRepository.findById("LAPTOP-001").orElseThrow();
                    assertThat(inventory.getQuantityReserved()).isEqualTo(2);
                    assertThat(inventory.getEffectiveAvailable()).isEqualTo(48);

                    // Verify event was marked as processed
                    assertThat(processedEventRepository.count()).isEqualTo(1);
                });

        // Step 4: Verify inventory via REST API
        mockMvc.perform(get("/api/inventory/LAPTOP-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityReserved").value(2))
                .andExpect(jsonPath("$.effectiveAvailable").value(48));

        // Step 5: Verify order via REST API
        mockMvc.perform(get("/api/orders/" + createdOrder.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdOrder.id()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("Multiple orders for same product should reserve stock correctly")
    void shouldHandleMultipleOrdersCorrectly() throws Exception {
        // Set up inventory
        InventoryController.AddInventoryRequest inventoryRequest = new InventoryController.AddInventoryRequest(
                "PHONE-001",
                "Smartphone",
                100
        );

        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inventoryRequest)))
                .andExpect(status().isCreated());

        // Create first order
        OrderController.CreateOrderRequest order1 = new OrderController.CreateOrderRequest(
                "CUST-001",
                List.of(new OrderController.CreateOrderItemRequest("PHONE-001", "Smartphone", 10, new BigDecimal("599.99")))
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order1)))
                .andExpect(status().isCreated());

        // Create second order
        OrderController.CreateOrderRequest order2 = new OrderController.CreateOrderRequest(
                "CUST-002",
                List.of(new OrderController.CreateOrderItemRequest("PHONE-001", "Smartphone", 15, new BigDecimal("599.99")))
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order2)))
                .andExpect(status().isCreated());

        // Wait for both orders to be processed
        await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    var inventory = inventoryRepository.findById("PHONE-001").orElseThrow();
                    // 10 + 15 = 25 reserved
                    assertThat(inventory.getQuantityReserved()).isEqualTo(25);
                    assertThat(inventory.getEffectiveAvailable()).isEqualTo(75);
                });
    }

    @Test
    @DisplayName("Should update order status via REST API")
    void shouldUpdateOrderStatus() throws Exception {
        // Create order directly in database (skip Kafka for this test)
        Order order = Order.builder()
                .id("TEST-ORDER-001")
                .customerId("CUST-001")
                .status(Order.OrderStatus.PENDING)
                .totalAmount(new BigDecimal("100.00"))
                .build();
        orderRepository.save(order);

        // Update status to CONFIRMED
        OrderController.UpdateStatusRequest statusRequest = new OrderController.UpdateStatusRequest(
                Order.OrderStatus.CONFIRMED
        );

        mockMvc.perform(patch("/api/orders/TEST-ORDER-001/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // Verify in database
        Order updatedOrder = orderRepository.findById("TEST-ORDER-001").orElseThrow();
        assertThat(updatedOrder.getStatus()).isEqualTo(Order.OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Should cancel order via REST API")
    void shouldCancelOrder() throws Exception {
        // Create order directly in database
        Order order = Order.builder()
                .id("CANCEL-ORDER-001")
                .customerId("CUST-001")
                .status(Order.OrderStatus.PENDING)
                .totalAmount(new BigDecimal("100.00"))
                .build();
        orderRepository.save(order);

        // Cancel order
        mockMvc.perform(post("/api/orders/CANCEL-ORDER-001/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("Should return 404 for non-existent order")
    void shouldReturn404ForNonExistentOrder() throws Exception {
        mockMvc.perform(get("/api/orders/NON-EXISTENT"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 404 for non-existent inventory item")
    void shouldReturn404ForNonExistentInventoryItem() throws Exception {
        mockMvc.perform(get("/api/inventory/NON-EXISTENT"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should get orders by customer")
    void shouldGetOrdersByCustomer() throws Exception {
        // Create orders for a customer
        orderRepository.save(Order.builder()
                .id("ORDER-A")
                .customerId("CUST-100")
                .status(Order.OrderStatus.PENDING)
                .totalAmount(new BigDecimal("50.00"))
                .build());

        orderRepository.save(Order.builder()
                .id("ORDER-B")
                .customerId("CUST-100")
                .status(Order.OrderStatus.CONFIRMED)
                .totalAmount(new BigDecimal("75.00"))
                .build());

        // Get orders by customer
        mockMvc.perform(get("/api/orders/customer/CUST-100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("Should get all inventory items")
    void shouldGetAllInventoryItems() throws Exception {
        // Add multiple inventory items
        inventoryRepository.save(com.example.orderprocessing.domain.entity.InventoryItem.builder()
                .productId("PROD-A")
                .productName("Product A")
                .quantityAvailable(100)
                .quantityReserved(0)
                .build());

        inventoryRepository.save(com.example.orderprocessing.domain.entity.InventoryItem.builder()
                .productId("PROD-B")
                .productName("Product B")
                .quantityAvailable(50)
                .quantityReserved(0)
                .build());

        mockMvc.perform(get("/api/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("Should get low stock items")
    void shouldGetLowStockItems() throws Exception {
        // Add items with varying stock levels
        inventoryRepository.save(com.example.orderprocessing.domain.entity.InventoryItem.builder()
                .productId("HIGH-STOCK")
                .productName("High Stock Product")
                .quantityAvailable(100)
                .quantityReserved(0)
                .build());

        inventoryRepository.save(com.example.orderprocessing.domain.entity.InventoryItem.builder()
                .productId("LOW-STOCK")
                .productName("Low Stock Product")
                .quantityAvailable(5)
                .quantityReserved(0)
                .build());

        mockMvc.perform(get("/api/inventory/low-stock?threshold=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].productId").value("LOW-STOCK"));
    }
}
