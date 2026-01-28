import { CheckoutState, CheckoutAction, CheckoutStep } from './types';

const STEP_ORDER: CheckoutStep[] = ['cart', 'shipping', 'payment', 'confirmation'];

export const initialState: CheckoutState = {
  step: 'cart',
  cart: [],
  shipping: null,
  payment: null,
  orderId: null,
};

export function checkoutReducer(state: CheckoutState, action: CheckoutAction): CheckoutState {
  switch (action.type) {
    case 'NEXT_STEP': {
      const currentIndex = STEP_ORDER.indexOf(state.step);
      if (currentIndex < STEP_ORDER.length - 1) {
        return { ...state, step: STEP_ORDER[currentIndex + 1] };
      }
      return state;
    }

    case 'PREV_STEP': {
      const currentIndex = STEP_ORDER.indexOf(state.step);
      if (currentIndex > 0) {
        return { ...state, step: STEP_ORDER[currentIndex - 1] };
      }
      return state;
    }

    case 'SET_CART':
      return { ...state, cart: action.payload };

    case 'SET_SHIPPING':
      return { ...state, shipping: action.payload };

    case 'SET_PAYMENT':
      return { ...state, payment: action.payload };

    case 'COMPLETE_ORDER':
      return { ...state, orderId: action.payload, step: 'confirmation' };

    case 'RESET':
      return initialState;

    default:
      return state;
  }
}
