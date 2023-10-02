package com.hivemq.bootstrap;

import com.hivemq.configuration.service.entity.Listener;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dominik Obermaier
 */
@Immutable
public class ListenerStartupInformation {

    private final boolean successful;
    private final @NotNull Listener listener;
    private final @Nullable Throwable exception;

    private ListenerStartupInformation(
            final boolean successful, final @NotNull Listener listener, final @Nullable Throwable exception) {

        checkNotNull(listener, "Original Listener must not be null");

        this.successful = successful;
        this.listener = listener;
        this.exception = exception;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public @NotNull Listener getListener() {
        return listener;
    }

    public @NotNull Optional<Throwable> getException() {
        return Optional.ofNullable(exception);
    }

    public static ListenerStartupInformation successfulListenerStartup(final @NotNull Listener listener) {
        return new ListenerStartupInformation(true, listener, null);
    }

    public static ListenerStartupInformation failedListenerStartup(
            final @NotNull Listener listener, final @Nullable Throwable exception) {
        return new ListenerStartupInformation(false, listener, exception);
    }
}
