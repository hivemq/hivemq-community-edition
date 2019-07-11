package com.hivemq.extensions.services.general;

import com.hivemq.extension.sdk.api.services.general.IterationContext;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Christoph Schäbel
 */
public class IterationContextImpl implements IterationContext {

    private final AtomicBoolean aborted = new AtomicBoolean(false);

    @Override
    public void abortIteration() {
        aborted.set(true);
    }

    public boolean isAborted() {
        return aborted.get();
    }
}
