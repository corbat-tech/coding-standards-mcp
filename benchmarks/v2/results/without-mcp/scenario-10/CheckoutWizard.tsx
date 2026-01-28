import React, { useReducer, useState } from 'react';
import { checkoutReducer, initialState } from './checkoutReducer';
import { ShippingInfo, PaymentInfo, CartItem } from './types';
import { validateCart, validateShipping, validatePayment, ValidationErrors } from './validation';

interface CheckoutWizardProps {
  initialCart?: CartItem[];
  onComplete?: (orderId: string) => void;
}

export const CheckoutWizard: React.FC<CheckoutWizardProps> = ({
  initialCart = [],
  onComplete,
}) => {
  const [state, dispatch] = useReducer(checkoutReducer, {
    ...initialState,
    cart: initialCart,
  });
  const [errors, setErrors] = useState<ValidationErrors>({});

  const [shippingForm, setShippingForm] = useState<ShippingInfo>({
    fullName: '',
    address: '',
    city: '',
    zipCode: '',
    country: '',
  });

  const [paymentForm, setPaymentForm] = useState<PaymentInfo>({
    cardNumber: '',
    expiryDate: '',
    cvv: '',
    cardholderName: '',
  });

  const handleNext = () => {
    let validationErrors: ValidationErrors = {};

    switch (state.step) {
      case 'cart':
        validationErrors = validateCart(state.cart);
        break;
      case 'shipping':
        validationErrors = validateShipping(shippingForm);
        if (Object.keys(validationErrors).length === 0) {
          dispatch({ type: 'SET_SHIPPING', payload: shippingForm });
        }
        break;
      case 'payment':
        validationErrors = validatePayment(paymentForm);
        if (Object.keys(validationErrors).length === 0) {
          dispatch({ type: 'SET_PAYMENT', payload: paymentForm });
          const orderId = `ORD-${Date.now()}`;
          dispatch({ type: 'COMPLETE_ORDER', payload: orderId });
          onComplete?.(orderId);
          return;
        }
        break;
    }

    setErrors(validationErrors);

    if (Object.keys(validationErrors).length === 0) {
      dispatch({ type: 'NEXT_STEP' });
    }
  };

  const handleBack = () => {
    setErrors({});
    dispatch({ type: 'PREV_STEP' });
  };

  const getTotal = () => {
    return state.cart.reduce((sum, item) => sum + item.price * item.quantity, 0);
  };

  return (
    <div data-testid="checkout-wizard">
      <div data-testid="step-indicator">
        Step: {state.step}
      </div>

      {state.step === 'cart' && (
        <div data-testid="cart-step">
          <h2>Your Cart</h2>
          {state.cart.length === 0 ? (
            <p>Your cart is empty</p>
          ) : (
            <ul>
              {state.cart.map((item) => (
                <li key={item.id}>
                  {item.name} x {item.quantity} - ${item.price * item.quantity}
                </li>
              ))}
            </ul>
          )}
          <p>Total: ${getTotal()}</p>
          {errors.cart && <span data-testid="cart-error">{errors.cart}</span>}
        </div>
      )}

      {state.step === 'shipping' && (
        <div data-testid="shipping-step">
          <h2>Shipping Information</h2>
          <input
            placeholder="Full Name"
            value={shippingForm.fullName}
            onChange={(e) => setShippingForm({ ...shippingForm, fullName: e.target.value })}
            data-testid="shipping-fullname"
          />
          {errors.fullName && <span data-testid="fullname-error">{errors.fullName}</span>}

          <input
            placeholder="Address"
            value={shippingForm.address}
            onChange={(e) => setShippingForm({ ...shippingForm, address: e.target.value })}
            data-testid="shipping-address"
          />
          {errors.address && <span data-testid="address-error">{errors.address}</span>}

          <input
            placeholder="City"
            value={shippingForm.city}
            onChange={(e) => setShippingForm({ ...shippingForm, city: e.target.value })}
            data-testid="shipping-city"
          />
          {errors.city && <span data-testid="city-error">{errors.city}</span>}

          <input
            placeholder="ZIP Code"
            value={shippingForm.zipCode}
            onChange={(e) => setShippingForm({ ...shippingForm, zipCode: e.target.value })}
            data-testid="shipping-zipcode"
          />
          {errors.zipCode && <span data-testid="zipcode-error">{errors.zipCode}</span>}

          <input
            placeholder="Country"
            value={shippingForm.country}
            onChange={(e) => setShippingForm({ ...shippingForm, country: e.target.value })}
            data-testid="shipping-country"
          />
          {errors.country && <span data-testid="country-error">{errors.country}</span>}
        </div>
      )}

      {state.step === 'payment' && (
        <div data-testid="payment-step">
          <h2>Payment Information</h2>
          <input
            placeholder="Card Number"
            value={paymentForm.cardNumber}
            onChange={(e) => setPaymentForm({ ...paymentForm, cardNumber: e.target.value })}
            data-testid="payment-cardnumber"
          />
          {errors.cardNumber && <span data-testid="cardnumber-error">{errors.cardNumber}</span>}

          <input
            placeholder="MM/YY"
            value={paymentForm.expiryDate}
            onChange={(e) => setPaymentForm({ ...paymentForm, expiryDate: e.target.value })}
            data-testid="payment-expiry"
          />
          {errors.expiryDate && <span data-testid="expiry-error">{errors.expiryDate}</span>}

          <input
            placeholder="CVV"
            value={paymentForm.cvv}
            onChange={(e) => setPaymentForm({ ...paymentForm, cvv: e.target.value })}
            data-testid="payment-cvv"
          />
          {errors.cvv && <span data-testid="cvv-error">{errors.cvv}</span>}

          <input
            placeholder="Cardholder Name"
            value={paymentForm.cardholderName}
            onChange={(e) => setPaymentForm({ ...paymentForm, cardholderName: e.target.value })}
            data-testid="payment-cardholder"
          />
          {errors.cardholderName && <span data-testid="cardholder-error">{errors.cardholderName}</span>}
        </div>
      )}

      {state.step === 'confirmation' && (
        <div data-testid="confirmation-step">
          <h2>Order Confirmed!</h2>
          <p>Order ID: {state.orderId}</p>
          <p>Thank you for your purchase!</p>
        </div>
      )}

      <div>
        {state.step !== 'cart' && state.step !== 'confirmation' && (
          <button onClick={handleBack} data-testid="back-button">
            Back
          </button>
        )}
        {state.step !== 'confirmation' && (
          <button onClick={handleNext} data-testid="next-button">
            {state.step === 'payment' ? 'Complete Order' : 'Next'}
          </button>
        )}
      </div>
    </div>
  );
};
