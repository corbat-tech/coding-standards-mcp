import { Order } from './Order';

/**
 * Context shared across saga steps
 */
export interface SagaContextData {
  readonly orderId: string;
  readonly order?: Order;
  readonly inventoryReservationId?: string;
  readonly paymentTransactionId?: string;
  readonly shipmentTrackingId?: string;
  readonly metadata: Record<string, unknown>;
}

export class SagaContext implements SagaContextData {
  readonly orderId: string;
  readonly order?: Order;
  readonly inventoryReservationId?: string;
  readonly paymentTransactionId?: string;
  readonly shipmentTrackingId?: string;
  readonly metadata: Record<string, unknown>;

  private constructor(data: SagaContextData) {
    this.orderId = data.orderId;
    this.order = data.order;
    this.inventoryReservationId = data.inventoryReservationId;
    this.paymentTransactionId = data.paymentTransactionId;
    this.shipmentTrackingId = data.shipmentTrackingId;
    this.metadata = data.metadata;
  }

  static create(orderId: string): SagaContext {
    return new SagaContext({
      orderId,
      metadata: {}
    });
  }

  withOrder(order: Order): SagaContext {
    return new SagaContext({ ...this, order });
  }

  withInventoryReservation(reservationId: string): SagaContext {
    return new SagaContext({ ...this, inventoryReservationId: reservationId });
  }

  withPaymentTransaction(transactionId: string): SagaContext {
    return new SagaContext({ ...this, paymentTransactionId: transactionId });
  }

  withShipmentTracking(trackingId: string): SagaContext {
    return new SagaContext({ ...this, shipmentTrackingId: trackingId });
  }

  withMetadata(key: string, value: unknown): SagaContext {
    return new SagaContext({
      ...this,
      metadata: { ...this.metadata, [key]: value }
    });
  }
}
