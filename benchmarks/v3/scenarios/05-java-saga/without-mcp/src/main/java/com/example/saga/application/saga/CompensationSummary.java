package com.example.saga.application.saga;

import java.util.Collections;
import java.util.List;

/**
 * Summary of compensation actions performed during saga rollback.
 */
public final class CompensationSummary {

    private final boolean triggered;
    private final List<String> completedCompensations;
    private final List<String> failedCompensations;

    private CompensationSummary(boolean triggered, List<String> completedCompensations,
                                List<String> failedCompensations) {
        this.triggered = triggered;
        this.completedCompensations = Collections.unmodifiableList(completedCompensations);
        this.failedCompensations = Collections.unmodifiableList(failedCompensations);
    }

    /**
     * Creates an empty summary (no compensation was triggered).
     */
    public static CompensationSummary empty() {
        return new CompensationSummary(false, Collections.emptyList(), Collections.emptyList());
    }

    /**
     * Creates a summary with the results of compensation actions.
     */
    public static CompensationSummary of(List<String> completed, List<String> failed) {
        return new CompensationSummary(true, completed, failed);
    }

    public boolean isTriggered() {
        return triggered;
    }

    public List<String> getCompletedCompensations() {
        return completedCompensations;
    }

    public List<String> getFailedCompensations() {
        return failedCompensations;
    }

    public boolean hasFailedCompensations() {
        return !failedCompensations.isEmpty();
    }

    @Override
    public String toString() {
        return "CompensationSummary{" +
               "triggered=" + triggered +
               ", completedCompensations=" + completedCompensations +
               ", failedCompensations=" + failedCompensations +
               '}';
    }
}
