import { CartItem, ShippingInfo, PaymentInfo } from './types';

export interface ValidationErrors {
  [key: string]: string;
}

export function validateCart(cart: CartItem[]): ValidationErrors {
  const errors: ValidationErrors = {};

  if (cart.length === 0) {
    errors.cart = 'Cart is empty';
  }

  return errors;
}

export function validateShipping(shipping: ShippingInfo | null): ValidationErrors {
  const errors: ValidationErrors = {};

  if (!shipping) {
    errors.shipping = 'Shipping information is required';
    return errors;
  }

  if (!shipping.fullName.trim()) {
    errors.fullName = 'Full name is required';
  }

  if (!shipping.address.trim()) {
    errors.address = 'Address is required';
  }

  if (!shipping.city.trim()) {
    errors.city = 'City is required';
  }

  if (!shipping.zipCode.trim()) {
    errors.zipCode = 'ZIP code is required';
  } else if (!/^\d{5}(-\d{4})?$/.test(shipping.zipCode)) {
    errors.zipCode = 'Invalid ZIP code format';
  }

  if (!shipping.country.trim()) {
    errors.country = 'Country is required';
  }

  return errors;
}

export function validatePayment(payment: PaymentInfo | null): ValidationErrors {
  const errors: ValidationErrors = {};

  if (!payment) {
    errors.payment = 'Payment information is required';
    return errors;
  }

  if (!payment.cardNumber.trim()) {
    errors.cardNumber = 'Card number is required';
  } else if (!/^\d{16}$/.test(payment.cardNumber.replace(/\s/g, ''))) {
    errors.cardNumber = 'Invalid card number';
  }

  if (!payment.expiryDate.trim()) {
    errors.expiryDate = 'Expiry date is required';
  } else if (!/^(0[1-9]|1[0-2])\/\d{2}$/.test(payment.expiryDate)) {
    errors.expiryDate = 'Invalid expiry date (MM/YY)';
  }

  if (!payment.cvv.trim()) {
    errors.cvv = 'CVV is required';
  } else if (!/^\d{3,4}$/.test(payment.cvv)) {
    errors.cvv = 'Invalid CVV';
  }

  if (!payment.cardholderName.trim()) {
    errors.cardholderName = 'Cardholder name is required';
  }

  return errors;
}
