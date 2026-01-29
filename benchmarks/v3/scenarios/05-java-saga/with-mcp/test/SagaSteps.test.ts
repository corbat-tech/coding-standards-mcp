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
import { CreateOrderStep } from '../application/saga/CreateOrderStep';
import { ReserveInventoryStep } from '../application/saga/ReserveInventoryStep';
import { ProcessPaymentStep } from '../application/saga/ProcessPaymentStep';
import { ShipOrderStep } from '../application/saga/ShipOrderStep';

describe('CreateOrderStep', () => {
  let step: CreateOrderStep;
  let orderService: OrderService;
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
    orderService = {
      createOrder: vi.fn().mockResolvedValue(testOrder),
      cancelOrder: vi.fn().mockResolvedValue(undefined),
      getOrder: vi.fn().mockResolvedValue(testOrder)
    };
    step = new CreateOrderStep(orderService);
  });

  it('should execute successfully and return order', async () => {
    const context = SagaContext.create('order-123')
      .withMetadata('customerId', 'customer-456')
      .withMetadata('items', testItems)
      .withMetadata('totalAmount', 100);

    const result = await step.execute(context);

    expect(result.success).toBe(true);
    expect(result.data?.order).toBeDefined();
    expect(orderService.createOrder).toHaveBeenCalledWith(
      'customer-456',
      testItems,
      100
    );
  });

  it('should return failure on error', async () => {
    vi.mocked(orderService.createOrder).mockRejectedValue(
      new Error('DB connection failed')
    );
    const context = SagaContext.create('order-123')
      .withMetadata('customerId', 'customer-456')
      .withMetadata('items', testItems)
      .withMetadata('totalAmount', 100);

    const result = await step.execute(context);

    expect(result.success).toBe(false);
    expect(result.error).toBeInstanceOf(SagaError);
    expect(result.error?.code).toBe('ORDER_CREATION_FAILED');
  });

  it('should compensate by cancelling order', async () => {
    const context = SagaContext.create('order-123').withOrder(testOrder);

    const result = await step.compensate(context);

    expect(result.success).toBe(true);
    expect(orderService.cancelOrder).toHaveBeenCalledWith('order-123');
  });
});

describe('ReserveInventoryStep', () => {
  let step: ReserveInventoryStep;
  let inventoryService: InventoryService;
  const testItems: OrderItem[] = [
    { productId: 'prod-1', quantity: 2, unitPrice: 50 }
  ];

  beforeEach(() => {
    inventoryService = {
      reserveInventory: vi.fn().mockResolvedValue('reservation-789'),
      releaseInventory: vi.fn().mockResolvedValue(undefined),
      checkAvailability: vi.fn().mockResolvedValue(true)
    };
    step = new ReserveInventoryStep(inventoryService);
  });

  it('should execute successfully and return reservation id', async () => {
    const order = Order.create({
      id: 'order-123',
      customerId: 'customer-456',
      items: testItems,
      totalAmount: 100
    });
    const context = SagaContext.create('order-123').withOrder(order);

    const result = await step.execute(context);

    expect(result.success).toBe(true);
    expect(result.data?.reservationId).toBe('reservation-789');
  });

  it('should return failure when order is missing', async () => {
    const context = SagaContext.create('order-123');

    const result = await step.execute(context);

    expect(result.success).toBe(false);
    expect(result.error?.code).toBe('ORDER_NOT_FOUND');
  });

  it('should compensate by releasing inventory', async () => {
    const context = SagaContext.create('order-123').withInventoryReservation(
      'reservation-789'
    );

    const result = await step.compensate(context);

    expect(result.success).toBe(true);
    expect(inventoryService.releaseInventory).toHaveBeenCalledWith(
      'reservation-789'
    );
  });
});

describe('ProcessPaymentStep', () => {
  let step: ProcessPaymentStep;
  let paymentService: PaymentService;

  beforeEach(() => {
    paymentService = {
      processPayment: vi.fn().mockResolvedValue('txn-101'),
      refundPayment: vi.fn().mockResolvedValue(undefined)
    };
    step = new ProcessPaymentStep(paymentService);
  });

  it('should execute successfully and return transaction id', async () => {
    const order = Order.create({
      id: 'order-123',
      customerId: 'customer-456',
      items: [],
      totalAmount: 100
    });
    const context = SagaContext.create('order-123').withOrder(order);

    const result = await step.execute(context);

    expect(result.success).toBe(true);
    expect(result.data?.transactionId).toBe('txn-101');
    expect(paymentService.processPayment).toHaveBeenCalledWith(
      'order-123',
      'customer-456',
      100
    );
  });

  it('should return failure when order is missing', async () => {
    const context = SagaContext.create('order-123');

    const result = await step.execute(context);

    expect(result.success).toBe(false);
    expect(result.error?.code).toBe('ORDER_NOT_FOUND');
  });

  it('should compensate by refunding payment', async () => {
    const context = SagaContext.create('order-123').withPaymentTransaction(
      'txn-101'
    );

    const result = await step.compensate(context);

    expect(result.success).toBe(true);
    expect(paymentService.refundPayment).toHaveBeenCalledWith('txn-101');
  });
});

describe('ShipOrderStep', () => {
  let step: ShipOrderStep;
  let shippingService: ShippingService;

  beforeEach(() => {
    shippingService = {
      createShipment: vi.fn().mockResolvedValue('tracking-202'),
      cancelShipment: vi.fn().mockResolvedValue(undefined)
    };
    step = new ShipOrderStep(shippingService);
  });

  it('should execute successfully and return tracking id', async () => {
    const order = Order.create({
      id: 'order-123',
      customerId: 'customer-456',
      items: [],
      totalAmount: 100
    });
    const context = SagaContext.create('order-123').withOrder(order);

    const result = await step.execute(context);

    expect(result.success).toBe(true);
    expect(result.data?.trackingId).toBe('tracking-202');
    expect(shippingService.createShipment).toHaveBeenCalledWith(
      'order-123',
      'customer-456'
    );
  });

  it('should return failure when order is missing', async () => {
    const context = SagaContext.create('order-123');

    const result = await step.execute(context);

    expect(result.success).toBe(false);
    expect(result.error?.code).toBe('ORDER_NOT_FOUND');
  });

  it('should compensate by cancelling shipment', async () => {
    const context = SagaContext.create('order-123').withShipmentTracking(
      'tracking-202'
    );

    const result = await step.compensate(context);

    expect(result.success).toBe(true);
    expect(shippingService.cancelShipment).toHaveBeenCalledWith('tracking-202');
  });
});
