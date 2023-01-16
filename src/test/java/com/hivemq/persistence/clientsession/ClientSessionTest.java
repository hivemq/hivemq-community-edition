package com.hivemq.persistence.clientsession;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ClientSessionTest {

    @Test
    public void isExpired_whenDisconnectedAndTimeSinceDisconnectIsLargerThanExpiryInterval_thenSessionIsExpired() {
        final ClientSession session = new ClientSession(false, 10);
        assertTrue(session.isExpired(10001));
    }

    @Test
    public void isExpired_whenDisconnectedAndTimeSinceDisconnectIsEqualToExpiryInterval_thenSessionIsExpired() {
        final ClientSession session = new ClientSession(false, 10);
        assertTrue(session.isExpired(10000));
    }

    @Test
    public void isExpired_whenDisconnectedAndTimeSinceDisconnectIsSmallerThanExpiryInterval_thenSessionIsNotExpired() {
        final ClientSession session = new ClientSession(false, 10);
        assertFalse(session.isExpired(9999));
    }

    @Test
    public void isExpired_whenSessionIsConnected_thenSessionIsNotExpired() {
        final ClientSession session = new ClientSession(true, 10);
        assertFalse(session.isExpired(11000));
    }
}