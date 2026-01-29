package domain.exception;

/**
 * Exception thrown when an invalid order state transition is attempted.
 */
public class InvalidOrderStateException extends DomainException {

    public InvalidOrderStateException(String message) {
        super(message);
    }
}
