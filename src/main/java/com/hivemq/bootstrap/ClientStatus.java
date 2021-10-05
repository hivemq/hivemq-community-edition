package com.hivemq.bootstrap;

import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.EnumSet;

/**
 * @author Abdullah Imal
 */
public enum ClientStatus {

    CONNECTING,

    AUTHENTICATING,
    RE_AUTHENTICATING,
    AUTHENTICATED,

    DISCONNECTED_GRACEFULLY,
    DISCONNECTED_UNGRACEFULLY,
    TAKEN_OVER;

    private static final @NotNull EnumSet<ClientStatus> IMMUTABLE_STATUS =
            EnumSet.of(DISCONNECTED_GRACEFULLY, TAKEN_OVER);

    private static final @NotNull EnumSet<ClientStatus> UNAUTHENTICATED =
            EnumSet.of(CONNECTING, AUTHENTICATING, RE_AUTHENTICATING);

    private static final @NotNull EnumSet<ClientStatus> LEGACY_UNAUTHENTICATED = EnumSet.of(CONNECTING, AUTHENTICATING);

    private static final @NotNull EnumSet<ClientStatus> WAS_AUTHENTICATED =
            EnumSet.of(AUTHENTICATED, RE_AUTHENTICATING, TAKEN_OVER);

    public boolean immutableStatus() {
        return IMMUTABLE_STATUS.contains(this);
    }

    public boolean unauthenticated() {
        return UNAUTHENTICATED.contains(this);
    }

    public boolean legacyUnauthenticated() {
        return LEGACY_UNAUTHENTICATED.contains(this);
    }

    public boolean wasAuthenticated() {
        return WAS_AUTHENTICATED.contains(this);
    }
}
