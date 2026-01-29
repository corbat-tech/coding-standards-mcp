import { Order, OrderItem } from './Order';

/**
 * Service interfaces for saga steps (ports)
 */

export interface OrderService {
  createOrder(
    customerId: string,
    items: OrderItem[],
    totalAmount: number
  ): Promise<Order>;
  cancelOrder(orderId: string): Promise<void>;
  getOrder(orderId: string): Promise<Order | null>;
}

export interface InventoryService {
  reserveInventory(
    orderId: string,
    items: OrderItem[]
  ): Promise<string>; // returns reservationId
  releaseInventory(reservationId: string): Promise<void>;
  checkAvailability(items: OrderItem[]): Promise<boolean>;
}

export interface PaymentService {
  processPayment(
    orderId: string,
    customerId: string,
    amount: number
  ): Promise<string>; // returns transactionId
  refundPayment(transactionId: string): Promise<void>;
}

export interface ShippingService {
  createShipment(
    orderId: string,
    customerId: string
  ): Promise<string>; // returns trackingId
  cancelShipment(trackingId: string): Promise<void>;
}
