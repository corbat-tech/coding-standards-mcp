import React from 'react';
import { useCheckout, OrderSubmitter } from './useCheckout';
import { CartItem, ShippingInfo, PaymentInfo } from './types';
import { getStepIndex, isStepCompleted } from './checkoutMachine';

interface CheckoutWizardProps {
  initialCart?: CartItem[];
  orderSubmitter: OrderSubmitter;
}

export function CheckoutWizard({
  initialCart = [],
  orderSubmitter,
}: CheckoutWizardProps) {
  const {
    state,
    next,
    back,
    setShipping,
    setPayment,
    completeOrder,
    canGoBack,
  } = useCheckout(initialCart, orderSubmitter);

  const renderStep = () => {
    switch (state.currentStep) {
      case 'cart':
        return <CartStep cart={state.cart} errors={state.errors} />;
      case 'shipping':
        return (
          <ShippingStep
            shipping={state.shipping}
            onChange={setShipping}
            errors={state.errors}
          />
        );
      case 'payment':
        return (
          <PaymentStep
            payment={state.payment}
            onChange={setPayment}
            errors={state.errors}
          />
        );
      case 'confirmation':
        return <ConfirmationStep orderId={state.orderId!} />;
    }
  };

  return (
    <div className="checkout-wizard" data-testid="checkout-wizard">
      <StepIndicator currentStep={state.currentStep} state={state} />

      <div className="step-content">{renderStep()}</div>

      {state.currentStep !== 'confirmation' && (
        <div className="step-actions">
          {canGoBack && (
            <button type="button" onClick={back} data-testid="back-button">
              Back
            </button>
          )}
          <button
            type="button"
            onClick={state.currentStep === 'payment' ? completeOrder : next}
            data-testid="next-button"
          >
            {state.currentStep === 'payment' ? 'Place Order' : 'Continue'}
          </button>
        </div>
      )}
    </div>
  );
}

interface StepIndicatorProps {
  currentStep: string;
  state: ReturnType<typeof useCheckout>['state'];
}

function StepIndicator({ currentStep, state }: StepIndicatorProps) {
  const steps = ['cart', 'shipping', 'payment', 'confirmation'] as const;

  return (
    <div className="step-indicator" data-testid="step-indicator">
      {steps.map((step) => (
        <div
          key={step}
          className={`step ${step === currentStep ? 'active' : ''} ${
            isStepCompleted(state, step) ? 'completed' : ''
          }`}
          data-testid={`step-${step}`}
        >
          {getStepIndex(step) + 1}. {step.charAt(0).toUpperCase() + step.slice(1)}
        </div>
      ))}
    </div>
  );
}

interface CartStepProps {
  cart: CartItem[];
  errors: Record<string, string>;
}

function CartStep({ cart, errors }: CartStepProps) {
  const total = cart.reduce((sum, item) => sum + item.price * item.quantity, 0);

  return (
    <div data-testid="cart-step">
      <h2>Your Cart</h2>
      {errors.cart && <p className="error">{errors.cart}</p>}
      {cart.length === 0 ? (
        <p>Your cart is empty</p>
      ) : (
        <>
          <ul>
            {cart.map((item) => (
              <li key={item.id}>
                {item.name} x {item.quantity} - ${item.price * item.quantity}
              </li>
            ))}
          </ul>
          <p>Total: ${total}</p>
        </>
      )}
    </div>
  );
}

interface ShippingStepProps {
  shipping: ShippingInfo | null;
  onChange: (shipping: ShippingInfo) => void;
  errors: Record<string, string>;
}

function ShippingStep({ shipping, onChange, errors }: ShippingStepProps) {
  const current = shipping || {
    fullName: '',
    address: '',
    city: '',
    postalCode: '',
    country: '',
  };

  const handleChange = (field: keyof ShippingInfo, value: string) => {
    onChange({ ...current, [field]: value });
  };

  return (
    <div data-testid="shipping-step">
      <h2>Shipping Information</h2>
      <input
        type="text"
        placeholder="Full Name"
        value={current.fullName}
        onChange={(e) => handleChange('fullName', e.target.value)}
        data-testid="shipping-fullName"
      />
      {errors.fullName && <span className="error">{errors.fullName}</span>}

      <input
        type="text"
        placeholder="Address"
        value={current.address}
        onChange={(e) => handleChange('address', e.target.value)}
        data-testid="shipping-address"
      />
      {errors.address && <span className="error">{errors.address}</span>}

      <input
        type="text"
        placeholder="City"
        value={current.city}
        onChange={(e) => handleChange('city', e.target.value)}
        data-testid="shipping-city"
      />
      {errors.city && <span className="error">{errors.city}</span>}

      <input
        type="text"
        placeholder="Postal Code"
        value={current.postalCode}
        onChange={(e) => handleChange('postalCode', e.target.value)}
        data-testid="shipping-postalCode"
      />
      {errors.postalCode && <span className="error">{errors.postalCode}</span>}

      <input
        type="text"
        placeholder="Country"
        value={current.country}
        onChange={(e) => handleChange('country', e.target.value)}
        data-testid="shipping-country"
      />
      {errors.country && <span className="error">{errors.country}</span>}
    </div>
  );
}

interface PaymentStepProps {
  payment: PaymentInfo | null;
  onChange: (payment: PaymentInfo) => void;
  errors: Record<string, string>;
}

function PaymentStep({ payment, onChange, errors }: PaymentStepProps) {
  const current = payment || {
    cardNumber: '',
    expiryDate: '',
    cvv: '',
    cardholderName: '',
  };

  const handleChange = (field: keyof PaymentInfo, value: string) => {
    onChange({ ...current, [field]: value });
  };

  return (
    <div data-testid="payment-step">
      <h2>Payment Information</h2>
      <input
        type="text"
        placeholder="Card Number"
        value={current.cardNumber}
        onChange={(e) => handleChange('cardNumber', e.target.value)}
        data-testid="payment-cardNumber"
      />
      {errors.cardNumber && <span className="error">{errors.cardNumber}</span>}

      <input
        type="text"
        placeholder="MM/YY"
        value={current.expiryDate}
        onChange={(e) => handleChange('expiryDate', e.target.value)}
        data-testid="payment-expiryDate"
      />
      {errors.expiryDate && <span className="error">{errors.expiryDate}</span>}

      <input
        type="text"
        placeholder="CVV"
        value={current.cvv}
        onChange={(e) => handleChange('cvv', e.target.value)}
        data-testid="payment-cvv"
      />
      {errors.cvv && <span className="error">{errors.cvv}</span>}

      <input
        type="text"
        placeholder="Cardholder Name"
        value={current.cardholderName}
        onChange={(e) => handleChange('cardholderName', e.target.value)}
        data-testid="payment-cardholderName"
      />
      {errors.cardholderName && (
        <span className="error">{errors.cardholderName}</span>
      )}
    </div>
  );
}

interface ConfirmationStepProps {
  orderId: string;
}

function ConfirmationStep({ orderId }: ConfirmationStepProps) {
  return (
    <div data-testid="confirmation-step">
      <h2>Order Confirmed!</h2>
      <p>Thank you for your purchase.</p>
      <p>Order ID: {orderId}</p>
    </div>
  );
}
