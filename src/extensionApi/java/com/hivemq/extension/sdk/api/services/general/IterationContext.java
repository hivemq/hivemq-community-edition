package com.hivemq.extension.sdk.api.services.general;

/**
 * @author Christoph Schäbel
 * @since 4.2.0
 */
public interface IterationContext {

    /**
     * Aborts the iteration at the current step of the iteration.
     * <p>
     * No further callbacks will be executed and the result future will complete successfully as soon as the current
     * iteration callback returns
     */
    void abortIteration();

}
