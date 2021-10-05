package com.hivemq.bootstrap;

import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.EnumSet;

/**
 * @author Abdullah Imal
 */
public enum ClientState {

    CONNECTING,

    AUTHENTICATING,
    RE_AUTHENTICATING,
    AUTHENTICATED,

    DISCONNECTING,
    DISCONNECTED_UNSPECIFIED,
    DISCONNECTED_GRACEFULLY,
    DISCONNECTED_UNGRACEFULLY,
    TAKEN_OVER;

    private static final @NotNull EnumSet<ClientState> IMMUTABLE_STATUS =
            EnumSet.of(DISCONNECTED_GRACEFULLY, DISCONNECTED_UNGRACEFULLY, TAKEN_OVER);

    private static final @NotNull EnumSet<ClientState> UNAUTHENTICATED = EnumSet.of(CONNECTING, AUTHENTICATING);

    private static final @NotNull EnumSet<ClientState> DISCONNECTED =
            EnumSet.of(DISCONNECTED_GRACEFULLY, DISCONNECTED_UNGRACEFULLY, TAKEN_OVER);

    public boolean immutableStatus() {
        return IMMUTABLE_STATUS.contains(this);
    }

    public boolean unauthenticated() {
        return UNAUTHENTICATED.contains(this);
    }

    public boolean disconnected() {
        return DISCONNECTED.contains(this);
    }
}
