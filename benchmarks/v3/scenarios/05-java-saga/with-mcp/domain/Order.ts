/**
 * Order entity representing an order in the fulfillment process
 */
export interface OrderData {
  readonly id: string;
  readonly customerId: string;
  readonly items: ReadonlyArray<OrderItem>;
  readonly totalAmount: number;
  readonly status: OrderStatus;
  readonly createdAt: Date;
}

export interface OrderItem {
  readonly productId: string;
  readonly quantity: number;
  readonly unitPrice: number;
}

export type OrderStatus =
  | 'PENDING'
  | 'CREATED'
  | 'INVENTORY_RESERVED'
  | 'PAYMENT_PROCESSED'
  | 'SHIPPED'
  | 'CANCELLED'
  | 'FAILED';

export class Order implements OrderData {
  readonly id: string;
  readonly customerId: string;
  readonly items: ReadonlyArray<OrderItem>;
  readonly totalAmount: number;
  readonly status: OrderStatus;
  readonly createdAt: Date;

  private constructor(data: OrderData) {
    this.id = data.id;
    this.customerId = data.customerId;
    this.items = data.items;
    this.totalAmount = data.totalAmount;
    this.status = data.status;
    this.createdAt = data.createdAt;
  }

  static create(data: Omit<OrderData, 'status' | 'createdAt'>): Order {
    return new Order({
      ...data,
      status: 'PENDING',
      createdAt: new Date()
    });
  }

  withStatus(status: OrderStatus): Order {
    return new Order({ ...this, status });
  }
}
