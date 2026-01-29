package com.example.saga.domain.exception;

import java.util.Collections;
import java.util.List;

/**
 * Exception thrown when compensation actions fail during saga rollback.
 */
public class CompensationException extends RuntimeException {

    private final SagaException originalError;
    private final List<String> failedCompensations;

    public CompensationException(String message, SagaException originalError, List<String> failedCompensations) {
        super(message);
        this.originalError = originalError;
        this.failedCompensations = Collections.unmodifiableList(failedCompensations);
    }

    public SagaException getOriginalError() {
        return originalError;
    }

    public List<String> getFailedCompensations() {
        return failedCompensations;
    }

    @Override
    public String toString() {
        return "CompensationException{" +
               "originalError=" + originalError +
               ", failedCompensations=" + failedCompensations +
               ", message='" + getMessage() + '\'' +
               '}';
    }
}
