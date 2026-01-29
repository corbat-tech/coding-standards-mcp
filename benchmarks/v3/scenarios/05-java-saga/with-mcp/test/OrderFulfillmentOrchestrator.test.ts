import { describe, it, expect, beforeEach, vi } from 'vitest';
import {
  Order,
  OrderItem,
  SagaContext,
  SagaError,
  OrderService,
  InventoryService,
  PaymentService,
  ShippingService
} from '../domain';
import { OrderFulfillmentOrchestrator } from '../application/saga/OrderFulfillmentOrchestrator';
import { CreateOrderStep } from '../application/saga/CreateOrderStep';
import { ReserveInventoryStep } from '../application/saga/ReserveInventoryStep';
import { ProcessPaymentStep } from '../application/saga/ProcessPaymentStep';
import { ShipOrderStep } from '../application/saga/ShipOrderStep';

describe('OrderFulfillmentOrchestrator', () => {
  let orchestrator: OrderFulfillmentOrchestrator;
  let orderService: OrderService;
  let inventoryService: InventoryService;
  let paymentService: PaymentService;
  let shippingService: ShippingService;

  const testItems: OrderItem[] = [
    { productId: 'prod-1', quantity: 2, unitPrice: 50 }
  ];

  const testOrder = Order.create({
    id: 'order-123',
    customerId: 'customer-456',
    items: testItems,
    totalAmount: 100
  });

  beforeEach(() => {
    // Create mock services
    orderService = {
      createOrder: vi.fn().mockResolvedValue(testOrder),
      cancelOrder: vi.fn().mockResolvedValue(undefined),
      getOrder: vi.fn().mockResolvedValue(testOrder)
    };

    inventoryService = {
      reserveInventory: vi.fn().mockResolvedValue('reservation-789'),
      releaseInventory: vi.fn().mockResolvedValue(undefined),
      checkAvailability: vi.fn().mockResolvedValue(true)
    };

    paymentService = {
      processPayment: vi.fn().mockResolvedValue('txn-101'),
      refundPayment: vi.fn().mockResolvedValue(undefined)
    };

    shippingService = {
      createShipment: vi.fn().mockResolvedValue('tracking-202'),
      cancelShipment: vi.fn().mockResolvedValue(undefined)
    };

    // Create saga steps
    const createOrderStep = new CreateOrderStep(orderService);
    const reserveInventoryStep = new ReserveInventoryStep(inventoryService);
    const processPaymentStep = new ProcessPaymentStep(paymentService);
    const shipOrderStep = new ShipOrderStep(shippingService);

    orchestrator = new OrderFulfillmentOrchestrator([
      createOrderStep,
      reserveInventoryStep,
      processPaymentStep,
      shipOrderStep
    ]);
  });

  describe('Happy Path', () => {
    it('should_complete_saga_successfully_when_all_steps_pass', async () => {
      const context = SagaContext.create('order-123')
        .withMetadata('customerId', 'customer-456')
        .withMetadata('items', testItems)
        .withMetadata('totalAmount', 100);

      const result = await orchestrator.execute(context);

      expect(result.success).toBe(true);
      expect(result.completedSteps).toEqual([
        'CreateOrder',
        'ReserveInventory',
        'ProcessPayment',
        'ShipOrder'
      ]);
      expect(result.failedStep).toBeUndefined();
      expect(result.error).toBeUndefined();
      expect(result.context.order).toBeDefined();
      expect(result.context.inventoryReservationId).toBe('reservation-789');
      expect(result.context.paymentTransactionId).toBe('txn-101');
      expect(result.context.shipmentTrackingId).toBe('tracking-202');
    });

    it('should_track_execution_state_through_saga', async () => {
      const context = SagaContext.create('order-123')
        .withMetadata('customerId', 'customer-456')
        .withMetadata('items', testItems)
        .withMetadata('totalAmount', 100);

      const result = await orchestrator.execute(context);

      expect(orderService.createOrder).toHaveBeenCalledTimes(1);
      expect(inventoryService.reserveInventory).toHaveBeenCalledTimes(1);
      expect(paymentService.processPayment).toHaveBeenCalledTimes(1);
      expect(shippingService.createShipment).toHaveBeenCalledTimes(1);
    });
  });

  describe('Failure Scenarios with Compensation', () => {
    it('should_compensate_when_create_order_fails', async () => {
      vi.mocked(orderService.createOrder).mockRejectedValue(
        new Error('Order creation failed')
      );

      const context = SagaContext.create('order-123')
        .withMetadata('customerId', 'customer-456')
        .withMetadata('items', testItems)
        .withMetadata('totalAmount', 100);

      const result = await orchestrator.execute(context);

      expect(result.success).toBe(false);
      expect(result.failedStep).toBe('CreateOrder');
      expect(result.completedSteps).toEqual([]);
      // No compensation needed since first step failed
      expect(result.compensationResults?.completedCompensations).toEqual([]);
    });

    it('should_compensate_create_order_when_reserve_inventory_fails', async () => {
      vi.mocked(inventoryService.reserveInventory).mockRejectedValue(
        new Error('Inventory not available')
      );

      const context = SagaContext.create('order-123')
        .withMetadata('customerId', 'customer-456')
        .withMetadata('items', testItems)
        .withMetadata('totalAmount', 100);

      const result = await orchestrator.execute(context);

      expect(result.success).toBe(false);
      expect(result.failedStep).toBe('ReserveInventory');
      expect(result.completedSteps).toEqual(['CreateOrder']);
      expect(result.compensationResults?.triggered).toBe(true);
      expect(result.compensationResults?.completedCompensations).toEqual([
        'CreateOrder'
      ]);
      expect(orderService.cancelOrder).toHaveBeenCalledWith('order-123');
    });

    it('should_compensate_inventory_and_order_when_payment_fails', async () => {
      vi.mocked(paymentService.processPayment).mockRejectedValue(
        new Error('Payment declined')
      );

      const context = SagaContext.create('order-123')
        .withMetadata('customerId', 'customer-456')
        .withMetadata('items', testItems)
        .withMetadata('totalAmount', 100);

      const result = await orchestrator.execute(context);

      expect(result.success).toBe(false);
      expect(result.failedStep).toBe('ProcessPayment');
      expect(result.completedSteps).toEqual(['CreateOrder', 'ReserveInventory']);
      expect(result.compensationResults?.triggered).toBe(true);
      expect(result.compensationResults?.completedCompensations).toContain(
        'ReserveInventory'
      );
      expect(result.compensationResults?.completedCompensations).toContain(
        'CreateOrder'
      );
      expect(inventoryService.releaseInventory).toHaveBeenCalledWith(
        'reservation-789'
      );
      expect(orderService.cancelOrder).toHaveBeenCalledWith('order-123');
    });

    it('should_compensate_all_steps_when_shipping_fails', async () => {
      vi.mocked(shippingService.createShipment).mockRejectedValue(
        new Error('Shipping unavailable')
      );

      const context = SagaContext.create('order-123')
        .withMetadata('customerId', 'customer-456')
        .withMetadata('items', testItems)
        .withMetadata('totalAmount', 100);

      const result = await orchestrator.execute(context);

      expect(result.success).toBe(false);
      expect(result.failedStep).toBe('ShipOrder');
      expect(result.completedSteps).toEqual([
        'CreateOrder',
        'ReserveInventory',
        'ProcessPayment'
      ]);
      expect(result.compensationResults?.triggered).toBe(true);
      expect(result.compensationResults?.completedCompensations).toHaveLength(3);
      expect(paymentService.refundPayment).toHaveBeenCalledWith('txn-101');
      expect(inventoryService.releaseInventory).toHaveBeenCalledWith(
        'reservation-789'
      );
      expect(orderService.cancelOrder).toHaveBeenCalledWith('order-123');
    });

    it('should_handle_compensation_failure_gracefully', async () => {
      vi.mocked(shippingService.createShipment).mockRejectedValue(
        new Error('Shipping unavailable')
      );
      vi.mocked(paymentService.refundPayment).mockRejectedValue(
        new Error('Refund failed')
      );

      const context = SagaContext.create('order-123')
        .withMetadata('customerId', 'customer-456')
        .withMetadata('items', testItems)
        .withMetadata('totalAmount', 100);

      const result = await orchestrator.execute(context);

      expect(result.success).toBe(false);
      expect(result.compensationResults?.triggered).toBe(true);
      expect(result.compensationResults?.failedCompensations).toContain(
        'ProcessPayment'
      );
      // Other compensations should still complete
      expect(result.compensationResults?.completedCompensations).toContain(
        'ReserveInventory'
      );
      expect(result.compensationResults?.completedCompensations).toContain(
        'CreateOrder'
      );
    });
  });

  describe('Orchestrator Configuration', () => {
    it('should_return_registered_steps', () => {
      const steps = orchestrator.getSteps();

      expect(steps).toHaveLength(4);
      expect(steps.map((s) => s.name)).toEqual([
        'CreateOrder',
        'ReserveInventory',
        'ProcessPayment',
        'ShipOrder'
      ]);
    });
  });
});
