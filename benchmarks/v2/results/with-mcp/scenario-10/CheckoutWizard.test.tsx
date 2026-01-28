import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { CheckoutWizard } from './CheckoutWizard';
import { OrderSubmitter } from './useCheckout';
import { CartItem } from './types';

const mockCart: CartItem[] = [
  { id: '1', name: 'Product 1', price: 10, quantity: 2 },
  { id: '2', name: 'Product 2', price: 25, quantity: 1 },
];

function createMockSubmitter(): OrderSubmitter {
  return {
    submit: vi.fn().mockResolvedValue('ORDER-12345'),
  };
}

describe('CheckoutWizard', () => {
  let submitter: OrderSubmitter;

  beforeEach(() => {
    submitter = createMockSubmitter();
  });

  describe('cart step', () => {
    it('should_render_cart_step_initially', () => {
      // Arrange & Act
      render(<CheckoutWizard initialCart={mockCart} orderSubmitter={submitter} />);

      // Assert
      expect(screen.getByTestId('cart-step')).toBeInTheDocument();
    });

    it('should_show_cart_items_when_has_items', () => {
      // Arrange & Act
      render(<CheckoutWizard initialCart={mockCart} orderSubmitter={submitter} />);

      // Assert
      expect(screen.getByText(/Product 1/)).toBeInTheDocument();
      expect(screen.getByText(/Product 2/)).toBeInTheDocument();
    });

    it('should_show_error_when_cart_empty', async () => {
      // Arrange
      render(<CheckoutWizard initialCart={[]} orderSubmitter={submitter} />);
      const user = userEvent.setup();

      // Act
      await user.click(screen.getByTestId('next-button'));

      // Assert
      expect(screen.getByText(/Cart cannot be empty/)).toBeInTheDocument();
    });

    it('should_proceed_to_shipping_when_cart_valid', async () => {
      // Arrange
      render(<CheckoutWizard initialCart={mockCart} orderSubmitter={submitter} />);
      const user = userEvent.setup();

      // Act
      await user.click(screen.getByTestId('next-button'));

      // Assert
      expect(screen.getByTestId('shipping-step')).toBeInTheDocument();
    });
  });

  describe('shipping step', () => {
    it('should_show_error_when_fields_empty', async () => {
      // Arrange
      render(<CheckoutWizard initialCart={mockCart} orderSubmitter={submitter} />);
      const user = userEvent.setup();
      await user.click(screen.getByTestId('next-button'));

      // Act
      await user.click(screen.getByTestId('next-button'));

      // Assert
      expect(screen.getByText(/Full name is required/)).toBeInTheDocument();
    });

    it('should_proceed_to_payment_when_valid', async () => {
      // Arrange
      render(<CheckoutWizard initialCart={mockCart} orderSubmitter={submitter} />);
      const user = userEvent.setup();
      await user.click(screen.getByTestId('next-button'));

      // Act
      await user.type(screen.getByTestId('shipping-fullName'), 'John Doe');
      await user.type(screen.getByTestId('shipping-address'), '123 Main St');
      await user.type(screen.getByTestId('shipping-city'), 'Springfield');
      await user.type(screen.getByTestId('shipping-postalCode'), '12345');
      await user.type(screen.getByTestId('shipping-country'), 'USA');
      await user.click(screen.getByTestId('next-button'));

      // Assert
      expect(screen.getByTestId('payment-step')).toBeInTheDocument();
    });

    it('should_go_back_to_cart_when_back_clicked', async () => {
      // Arrange
      render(<CheckoutWizard initialCart={mockCart} orderSubmitter={submitter} />);
      const user = userEvent.setup();
      await user.click(screen.getByTestId('next-button'));

      // Act
      await user.click(screen.getByTestId('back-button'));

      // Assert
      expect(screen.getByTestId('cart-step')).toBeInTheDocument();
    });
  });

  describe('payment step', () => {
    async function navigateToPayment(user: ReturnType<typeof userEvent.setup>) {
      await user.click(screen.getByTestId('next-button'));
      await user.type(screen.getByTestId('shipping-fullName'), 'John Doe');
      await user.type(screen.getByTestId('shipping-address'), '123 Main St');
      await user.type(screen.getByTestId('shipping-city'), 'Springfield');
      await user.type(screen.getByTestId('shipping-postalCode'), '12345');
      await user.type(screen.getByTestId('shipping-country'), 'USA');
      await user.click(screen.getByTestId('next-button'));
    }

    it('should_show_error_when_invalid_card', async () => {
      // Arrange
      render(<CheckoutWizard initialCart={mockCart} orderSubmitter={submitter} />);
      const user = userEvent.setup();
      await navigateToPayment(user);

      // Act
      await user.type(screen.getByTestId('payment-cardNumber'), '1234');
      await user.click(screen.getByTestId('next-button'));

      // Assert
      expect(screen.getByText(/Invalid card number/)).toBeInTheDocument();
    });

    it('should_submit_order_when_valid', async () => {
      // Arrange
      render(<CheckoutWizard initialCart={mockCart} orderSubmitter={submitter} />);
      const user = userEvent.setup();
      await navigateToPayment(user);

      // Act
      await user.type(screen.getByTestId('payment-cardNumber'), '4111111111111111');
      await user.type(screen.getByTestId('payment-expiryDate'), '12/25');
      await user.type(screen.getByTestId('payment-cvv'), '123');
      await user.type(screen.getByTestId('payment-cardholderName'), 'John Doe');
      await user.click(screen.getByTestId('next-button'));

      // Assert
      await waitFor(() => {
        expect(submitter.submit).toHaveBeenCalled();
      });
    });
  });

  describe('confirmation step', () => {
    async function completeCheckout(user: ReturnType<typeof userEvent.setup>) {
      await user.click(screen.getByTestId('next-button'));
      await user.type(screen.getByTestId('shipping-fullName'), 'John Doe');
      await user.type(screen.getByTestId('shipping-address'), '123 Main St');
      await user.type(screen.getByTestId('shipping-city'), 'Springfield');
      await user.type(screen.getByTestId('shipping-postalCode'), '12345');
      await user.type(screen.getByTestId('shipping-country'), 'USA');
      await user.click(screen.getByTestId('next-button'));
      await user.type(screen.getByTestId('payment-cardNumber'), '4111111111111111');
      await user.type(screen.getByTestId('payment-expiryDate'), '12/25');
      await user.type(screen.getByTestId('payment-cvv'), '123');
      await user.type(screen.getByTestId('payment-cardholderName'), 'John Doe');
      await user.click(screen.getByTestId('next-button'));
    }

    it('should_show_order_id_when_complete', async () => {
      // Arrange
      render(<CheckoutWizard initialCart={mockCart} orderSubmitter={submitter} />);
      const user = userEvent.setup();

      // Act
      await completeCheckout(user);

      // Assert
      await waitFor(() => {
        expect(screen.getByTestId('confirmation-step')).toBeInTheDocument();
        expect(screen.getByText(/ORDER-12345/)).toBeInTheDocument();
      });
    });

    it('should_hide_navigation_buttons_on_confirmation', async () => {
      // Arrange
      render(<CheckoutWizard initialCart={mockCart} orderSubmitter={submitter} />);
      const user = userEvent.setup();

      // Act
      await completeCheckout(user);

      // Assert
      await waitFor(() => {
        expect(screen.queryByTestId('next-button')).not.toBeInTheDocument();
        expect(screen.queryByTestId('back-button')).not.toBeInTheDocument();
      });
    });
  });

  describe('step indicator', () => {
    it('should_mark_current_step_as_active', () => {
      // Arrange & Act
      render(<CheckoutWizard initialCart={mockCart} orderSubmitter={submitter} />);

      // Assert
      expect(screen.getByTestId('step-cart')).toHaveClass('active');
    });
  });
});
