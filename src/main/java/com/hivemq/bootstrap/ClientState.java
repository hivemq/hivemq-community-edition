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
    CONNECT_FAILED,
    DISCONNECTED_UNSPECIFIED,
    DISCONNECTED_BY_CLIENT,
    DISCONNECTED_BY_SERVER,
    DISCONNECTED_TAKEN_OVER;

    private static final @NotNull EnumSet<ClientState> DISCONNECTED = EnumSet.of(DISCONNECTED_UNSPECIFIED,
            DISCONNECTED_BY_CLIENT, DISCONNECTED_BY_SERVER, DISCONNECTED_TAKEN_OVER, CONNECT_FAILED);

    private static final @NotNull EnumSet<ClientState> IMMUTABLE = DISCONNECTED;

    private static final @NotNull EnumSet<ClientState> UNAUTHENTICATED = EnumSet.of(CONNECTING, AUTHENTICATING);

    public boolean immutable() {
        return IMMUTABLE.contains(this);
    }

    public boolean unauthenticated() {
        return UNAUTHENTICATED.contains(this);
    }

    public boolean disconnected() {
        return DISCONNECTED.contains(this);
    }
}
