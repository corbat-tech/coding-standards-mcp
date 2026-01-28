import { useReducer, useCallback } from 'react';
import {
  CartItem,
  ShippingInfo,
  PaymentInfo,
  CheckoutState,
} from './types';
import {
  checkoutReducer,
  createInitialState,
  canGoNext,
  canGoBack,
} from './checkoutMachine';

export interface OrderSubmitter {
  submit(state: CheckoutState): Promise<string>;
}

export function useCheckout(
  initialCart: CartItem[] = [],
  orderSubmitter?: OrderSubmitter
) {
  const [state, dispatch] = useReducer(
    checkoutReducer,
    createInitialState(initialCart)
  );

  const next = useCallback(() => {
    dispatch({ type: 'NEXT' });
  }, []);

  const back = useCallback(() => {
    dispatch({ type: 'BACK' });
  }, []);

  const setCart = useCallback((cart: CartItem[]) => {
    dispatch({ type: 'SET_CART', payload: cart });
  }, []);

  const setShipping = useCallback((shipping: ShippingInfo) => {
    dispatch({ type: 'SET_SHIPPING', payload: shipping });
  }, []);

  const setPayment = useCallback((payment: PaymentInfo) => {
    dispatch({ type: 'SET_PAYMENT', payload: payment });
  }, []);

  const completeOrder = useCallback(async () => {
    if (!orderSubmitter) return;

    try {
      const orderId = await orderSubmitter.submit(state);
      dispatch({ type: 'COMPLETE_ORDER', payload: orderId });
    } catch {
      dispatch({
        type: 'SET_PAYMENT',
        payload: state.payment || {
          cardNumber: '',
          expiryDate: '',
          cvv: '',
          cardholderName: '',
        },
      });
    }
  }, [state, orderSubmitter]);

  const reset = useCallback(() => {
    dispatch({ type: 'RESET' });
  }, []);

  return {
    state,
    next,
    back,
    setCart,
    setShipping,
    setPayment,
    completeOrder,
    reset,
    canGoNext: canGoNext(state),
    canGoBack: canGoBack(state),
  };
}
