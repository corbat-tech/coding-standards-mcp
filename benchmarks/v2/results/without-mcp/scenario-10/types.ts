export type CheckoutStep = 'cart' | 'shipping' | 'payment' | 'confirmation';

export interface CartItem {
  id: string;
  name: string;
  price: number;
  quantity: number;
}

export interface ShippingInfo {
  fullName: string;
  address: string;
  city: string;
  zipCode: string;
  country: string;
}

export interface PaymentInfo {
  cardNumber: string;
  expiryDate: string;
  cvv: string;
  cardholderName: string;
}

export interface CheckoutState {
  step: CheckoutStep;
  cart: CartItem[];
  shipping: ShippingInfo | null;
  payment: PaymentInfo | null;
  orderId: string | null;
}

export type CheckoutAction =
  | { type: 'NEXT_STEP' }
  | { type: 'PREV_STEP' }
  | { type: 'SET_CART'; payload: CartItem[] }
  | { type: 'SET_SHIPPING'; payload: ShippingInfo }
  | { type: 'SET_PAYMENT'; payload: PaymentInfo }
  | { type: 'COMPLETE_ORDER'; payload: string }
  | { type: 'RESET' };
