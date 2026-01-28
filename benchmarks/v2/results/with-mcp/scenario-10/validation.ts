import {
  CartItem,
  ShippingInfo,
  PaymentInfo,
  StepValidation,
} from './types';

export function validateCart(cart: CartItem[]): StepValidation {
  const errors: Record<string, string> = {};

  if (cart.length === 0) {
    errors.cart = 'Cart cannot be empty';
  }

  const invalidItems = cart.filter((item) => item.quantity <= 0);
  if (invalidItems.length > 0) {
    errors.items = 'All items must have quantity greater than 0';
  }

  return {
    isValid: Object.keys(errors).length === 0,
    errors,
  };
}

export function validateShipping(shipping: ShippingInfo | null): StepValidation {
  const errors: Record<string, string> = {};

  if (!shipping) {
    errors.shipping = 'Shipping information is required';
    return { isValid: false, errors };
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

  if (!shipping.postalCode.trim()) {
    errors.postalCode = 'Postal code is required';
  }

  if (!shipping.country.trim()) {
    errors.country = 'Country is required';
  }

  return {
    isValid: Object.keys(errors).length === 0,
    errors,
  };
}

export function validatePayment(payment: PaymentInfo | null): StepValidation {
  const errors: Record<string, string> = {};

  if (!payment) {
    errors.payment = 'Payment information is required';
    return { isValid: false, errors };
  }

  const cardNumberClean = payment.cardNumber.replace(/\s/g, '');
  if (!/^\d{16}$/.test(cardNumberClean)) {
    errors.cardNumber = 'Invalid card number';
  }

  if (!/^\d{2}\/\d{2}$/.test(payment.expiryDate)) {
    errors.expiryDate = 'Invalid expiry date (MM/YY)';
  }

  if (!/^\d{3,4}$/.test(payment.cvv)) {
    errors.cvv = 'Invalid CVV';
  }

  if (!payment.cardholderName.trim()) {
    errors.cardholderName = 'Cardholder name is required';
  }

  return {
    isValid: Object.keys(errors).length === 0,
    errors,
  };
}
