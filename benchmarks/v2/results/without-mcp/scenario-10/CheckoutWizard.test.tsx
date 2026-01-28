import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { CheckoutWizard } from './CheckoutWizard';
import { CartItem } from './types';

const mockCart: CartItem[] = [
  { id: '1', name: 'Product 1', price: 10, quantity: 2 },
  { id: '2', name: 'Product 2', price: 25, quantity: 1 },
];

describe('CheckoutWizard', () => {
  describe('Cart Step', () => {
    it('renders cart step initially', () => {
      render(<CheckoutWizard initialCart={mockCart} />);
      expect(screen.getByTestId('cart-step')).toBeInTheDocument();
      expect(screen.getByText('Step: cart')).toBeInTheDocument();
    });

    it('displays cart items', () => {
      render(<CheckoutWizard initialCart={mockCart} />);
      expect(screen.getByText(/Product 1/)).toBeInTheDocument();
      expect(screen.getByText(/Product 2/)).toBeInTheDocument();
    });

    it('shows error when trying to proceed with empty cart', () => {
      render(<CheckoutWizard initialCart={[]} />);
      fireEvent.click(screen.getByTestId('next-button'));
      expect(screen.getByTestId('cart-error')).toHaveTextContent('Cart is empty');
    });

    it('proceeds to shipping step with valid cart', () => {
      render(<CheckoutWizard initialCart={mockCart} />);
      fireEvent.click(screen.getByTestId('next-button'));
      expect(screen.getByTestId('shipping-step')).toBeInTheDocument();
    });
  });

  describe('Shipping Step', () => {
    beforeEach(() => {
      render(<CheckoutWizard initialCart={mockCart} />);
      fireEvent.click(screen.getByTestId('next-button'));
    });

    it('shows validation errors for empty fields', () => {
      fireEvent.click(screen.getByTestId('next-button'));
      expect(screen.getByTestId('fullname-error')).toBeInTheDocument();
      expect(screen.getByTestId('address-error')).toBeInTheDocument();
    });

    it('validates ZIP code format', () => {
      fireEvent.change(screen.getByTestId('shipping-fullname'), { target: { value: 'John' } });
      fireEvent.change(screen.getByTestId('shipping-address'), { target: { value: '123 Main St' } });
      fireEvent.change(screen.getByTestId('shipping-city'), { target: { value: 'New York' } });
      fireEvent.change(screen.getByTestId('shipping-zipcode'), { target: { value: 'invalid' } });
      fireEvent.change(screen.getByTestId('shipping-country'), { target: { value: 'USA' } });

      fireEvent.click(screen.getByTestId('next-button'));
      expect(screen.getByTestId('zipcode-error')).toHaveTextContent('Invalid ZIP code format');
    });

    it('proceeds to payment with valid shipping info', () => {
      fireEvent.change(screen.getByTestId('shipping-fullname'), { target: { value: 'John Doe' } });
      fireEvent.change(screen.getByTestId('shipping-address'), { target: { value: '123 Main St' } });
      fireEvent.change(screen.getByTestId('shipping-city'), { target: { value: 'New York' } });
      fireEvent.change(screen.getByTestId('shipping-zipcode'), { target: { value: '12345' } });
      fireEvent.change(screen.getByTestId('shipping-country'), { target: { value: 'USA' } });

      fireEvent.click(screen.getByTestId('next-button'));
      expect(screen.getByTestId('payment-step')).toBeInTheDocument();
    });

    it('can go back to cart', () => {
      fireEvent.click(screen.getByTestId('back-button'));
      expect(screen.getByTestId('cart-step')).toBeInTheDocument();
    });
  });

  describe('Payment Step', () => {
    beforeEach(() => {
      render(<CheckoutWizard initialCart={mockCart} />);
      // Go through cart
      fireEvent.click(screen.getByTestId('next-button'));
      // Fill shipping
      fireEvent.change(screen.getByTestId('shipping-fullname'), { target: { value: 'John Doe' } });
      fireEvent.change(screen.getByTestId('shipping-address'), { target: { value: '123 Main St' } });
      fireEvent.change(screen.getByTestId('shipping-city'), { target: { value: 'New York' } });
      fireEvent.change(screen.getByTestId('shipping-zipcode'), { target: { value: '12345' } });
      fireEvent.change(screen.getByTestId('shipping-country'), { target: { value: 'USA' } });
      fireEvent.click(screen.getByTestId('next-button'));
    });

    it('shows validation errors for empty payment fields', () => {
      fireEvent.click(screen.getByTestId('next-button'));
      expect(screen.getByTestId('cardnumber-error')).toBeInTheDocument();
      expect(screen.getByTestId('expiry-error')).toBeInTheDocument();
      expect(screen.getByTestId('cvv-error')).toBeInTheDocument();
    });

    it('validates card number format', () => {
      fireEvent.change(screen.getByTestId('payment-cardnumber'), { target: { value: '123' } });
      fireEvent.change(screen.getByTestId('payment-expiry'), { target: { value: '12/25' } });
      fireEvent.change(screen.getByTestId('payment-cvv'), { target: { value: '123' } });
      fireEvent.change(screen.getByTestId('payment-cardholder'), { target: { value: 'John Doe' } });

      fireEvent.click(screen.getByTestId('next-button'));
      expect(screen.getByTestId('cardnumber-error')).toHaveTextContent('Invalid card number');
    });

    it('completes order with valid payment', () => {
      fireEvent.change(screen.getByTestId('payment-cardnumber'), { target: { value: '1234567890123456' } });
      fireEvent.change(screen.getByTestId('payment-expiry'), { target: { value: '12/25' } });
      fireEvent.change(screen.getByTestId('payment-cvv'), { target: { value: '123' } });
      fireEvent.change(screen.getByTestId('payment-cardholder'), { target: { value: 'John Doe' } });

      fireEvent.click(screen.getByTestId('next-button'));
      expect(screen.getByTestId('confirmation-step')).toBeInTheDocument();
    });
  });

  describe('Confirmation Step', () => {
    it('displays order ID after completion', () => {
      const onComplete = jest.fn();
      render(<CheckoutWizard initialCart={mockCart} onComplete={onComplete} />);

      // Complete the wizard
      fireEvent.click(screen.getByTestId('next-button'));

      fireEvent.change(screen.getByTestId('shipping-fullname'), { target: { value: 'John Doe' } });
      fireEvent.change(screen.getByTestId('shipping-address'), { target: { value: '123 Main St' } });
      fireEvent.change(screen.getByTestId('shipping-city'), { target: { value: 'New York' } });
      fireEvent.change(screen.getByTestId('shipping-zipcode'), { target: { value: '12345' } });
      fireEvent.change(screen.getByTestId('shipping-country'), { target: { value: 'USA' } });
      fireEvent.click(screen.getByTestId('next-button'));

      fireEvent.change(screen.getByTestId('payment-cardnumber'), { target: { value: '1234567890123456' } });
      fireEvent.change(screen.getByTestId('payment-expiry'), { target: { value: '12/25' } });
      fireEvent.change(screen.getByTestId('payment-cvv'), { target: { value: '123' } });
      fireEvent.change(screen.getByTestId('payment-cardholder'), { target: { value: 'John Doe' } });
      fireEvent.click(screen.getByTestId('next-button'));

      expect(screen.getByText(/Order ID:/)).toBeInTheDocument();
      expect(screen.getByText('Thank you for your purchase!')).toBeInTheDocument();
      expect(onComplete).toHaveBeenCalled();
    });
  });
});
