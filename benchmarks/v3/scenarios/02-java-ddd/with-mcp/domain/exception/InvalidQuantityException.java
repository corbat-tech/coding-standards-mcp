package domain.exception;

/**
 * Exception thrown when an invalid quantity is specified.
 */
public class InvalidQuantityException extends DomainException {

    public InvalidQuantityException(String message) {
        super(message);
    }
}
