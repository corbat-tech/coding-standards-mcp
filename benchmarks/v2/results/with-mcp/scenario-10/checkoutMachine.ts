import {
  CheckoutStep,
  CheckoutState,
  CartItem,
  ShippingInfo,
  PaymentInfo,
} from './types';
import { validateCart, validateShipping, validatePayment } from './validation';

type CheckoutAction =
  | { type: 'NEXT' }
  | { type: 'BACK' }
  | { type: 'SET_CART'; payload: CartItem[] }
  | { type: 'SET_SHIPPING'; payload: ShippingInfo }
  | { type: 'SET_PAYMENT'; payload: PaymentInfo }
  | { type: 'COMPLETE_ORDER'; payload: string }
  | { type: 'RESET' };

const STEP_ORDER: CheckoutStep[] = ['cart', 'shipping', 'payment', 'confirmation'];

const INITIAL_STATE: CheckoutState = {
  currentStep: 'cart',
  cart: [],
  shipping: null,
  payment: null,
  orderId: null,
  errors: {},
};

export function createInitialState(cart: CartItem[] = []): CheckoutState {
  return { ...INITIAL_STATE, cart };
}

export function checkoutReducer(
  state: CheckoutState,
  action: CheckoutAction
): CheckoutState {
  switch (action.type) {
    case 'NEXT':
      return handleNext(state);
    case 'BACK':
      return handleBack(state);
    case 'SET_CART':
      return { ...state, cart: action.payload, errors: {} };
    case 'SET_SHIPPING':
      return { ...state, shipping: action.payload, errors: {} };
    case 'SET_PAYMENT':
      return { ...state, payment: action.payload, errors: {} };
    case 'COMPLETE_ORDER':
      return {
        ...state,
        orderId: action.payload,
        currentStep: 'confirmation',
        errors: {},
      };
    case 'RESET':
      return INITIAL_STATE;
    default:
      return state;
  }
}

function handleNext(state: CheckoutState): CheckoutState {
  const validation = validateCurrentStep(state);

  if (!validation.isValid) {
    return { ...state, errors: validation.errors };
  }

  const currentIndex = STEP_ORDER.indexOf(state.currentStep);
  if (currentIndex < STEP_ORDER.length - 1) {
    return {
      ...state,
      currentStep: STEP_ORDER[currentIndex + 1],
      errors: {},
    };
  }

  return state;
}

function handleBack(state: CheckoutState): CheckoutState {
  const currentIndex = STEP_ORDER.indexOf(state.currentStep);
  if (currentIndex > 0) {
    return {
      ...state,
      currentStep: STEP_ORDER[currentIndex - 1],
      errors: {},
    };
  }
  return state;
}

function validateCurrentStep(state: CheckoutState) {
  switch (state.currentStep) {
    case 'cart':
      return validateCart(state.cart);
    case 'shipping':
      return validateShipping(state.shipping);
    case 'payment':
      return validatePayment(state.payment);
    default:
      return { isValid: true, errors: {} };
  }
}

export function canGoNext(state: CheckoutState): boolean {
  if (state.currentStep === 'confirmation') return false;
  return validateCurrentStep(state).isValid;
}

export function canGoBack(state: CheckoutState): boolean {
  return state.currentStep !== 'cart' && state.currentStep !== 'confirmation';
}

export function getStepIndex(step: CheckoutStep): number {
  return STEP_ORDER.indexOf(step);
}

export function isStepCompleted(state: CheckoutState, step: CheckoutStep): boolean {
  const currentIndex = getStepIndex(state.currentStep);
  const stepIndex = getStepIndex(step);
  return stepIndex < currentIndex;
}
