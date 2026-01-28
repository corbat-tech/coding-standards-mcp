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
  postalCode: string;
  country: string;
}

export interface PaymentInfo {
  cardNumber: string;
  expiryDate: string;
  cvv: string;
  cardholderName: string;
}

export interface CheckoutState {
  currentStep: CheckoutStep;
  cart: CartItem[];
  shipping: ShippingInfo | null;
  payment: PaymentInfo | null;
  orderId: string | null;
  errors: Record<string, string>;
}

export interface StepValidation {
  isValid: boolean;
  errors: Record<string, string>;
}
