package com.hivemq.extension.sdk.api.services.general;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.services.subscription.SubscriptionStore;

/**
 * A callback that can be passed to methods in extension stores (e.g. {@link SubscriptionStore}) to lazily iterate over
 * a potentially large result set.
 *
 * @author Christoph Schäbel
 * @since 4.2.0
 */
@FunctionalInterface
public interface IterationCallback<T> {

    /**
     * This method is called for every result that is part of the iteration.
     *
     * @param context an {@link IterationContext} that allows for interaction with the overlaying iteration mechanism.
     * @param value   the value for this single iteration.
     */
    void iterate(@NotNull IterationContext context, @NotNull T value);
}
