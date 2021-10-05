package com.hivemq.bootstrap;

/**
 * @author Abdullah Imal
 */
public enum ClientStatus {

    TCP_OPEN,
    CONNECTING,
    CONNECTED,
    AUTHENTICATING,
    RE_AUTHENTICATING,
    AUTHENTICATED,
    UNAUTHENTICATED,
    DISCONNECTED_GRACEFULLY,
    DISCONNECTED_UNGRACEFULLY,
    TAKEN_OVER
}
