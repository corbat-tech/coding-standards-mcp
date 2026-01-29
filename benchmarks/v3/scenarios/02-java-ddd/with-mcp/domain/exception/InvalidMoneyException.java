package domain.exception;

/**
 * Exception thrown when an invalid money operation is attempted.
 */
public class InvalidMoneyException extends DomainException {

    public InvalidMoneyException(String message) {
        super(message);
    }
}
