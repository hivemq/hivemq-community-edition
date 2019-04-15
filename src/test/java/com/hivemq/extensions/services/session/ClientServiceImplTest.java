/*
 * Copyright 2019 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hivemq.extensions.services.session;

import com.google.common.util.concurrent.Futures;
import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.services.exception.RateLimitExceededException;
import com.hivemq.extension.sdk.api.services.session.ClientService;
import com.hivemq.extension.sdk.api.services.session.SessionInformation;
import com.hivemq.extensions.services.PluginServiceRateLimitService;
import com.hivemq.mqtt.message.connect.MqttWillPublish;
import com.hivemq.persistence.clientsession.ClientSession;
import com.hivemq.persistence.clientsession.ClientSessionPersistence;
import com.hivemq.persistence.clientsession.ClientSessionWill;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import util.TestException;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static com.hivemq.persistence.clientsession.ClientSessionPersistenceImpl.DisconnectSource.EXTENSION;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
@SuppressWarnings("NullabilityAnnotations")
public class ClientServiceImplTest {

    private ClientService clientService;

    @Mock
    private ClientSessionPersistence clientSessionPersistence;

    @Mock
    private PluginServiceRateLimitService pluginServiceRateLimitService;

    private final String clientId = "clientID";
    private final long sessionExpiry = 123546L;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        clientService = new ClientServiceImpl(pluginServiceRateLimitService, clientSessionPersistence);
        when(pluginServiceRateLimitService.rateLimitExceeded()).thenReturn(false);
    }

    @Test(expected = RateLimitExceededException.class)
    public void test_get_session_limit_exceeded() throws Throwable {
        when(pluginServiceRateLimitService.rateLimitExceeded()).thenReturn(true);
        try {
            clientService.getSession(clientId).get();
        } catch (final InterruptedException | ExecutionException e) {
            throw e.getCause();
        }
    }

    @Test(expected = RateLimitExceededException.class)
    public void test_client_connected_limit_exceeded() throws Throwable {
        when(pluginServiceRateLimitService.rateLimitExceeded()).thenReturn(true);
        try {
            clientService.isClientConnected(clientId).get();
        } catch (final InterruptedException | ExecutionException e) {
            throw e.getCause();
        }
    }

    @Test(expected = RateLimitExceededException.class)
    public void test_disconnect_client_do_not_prevent_lwt_limit_exceeded() throws Throwable {
        when(pluginServiceRateLimitService.rateLimitExceeded()).thenReturn(true);
        try {
            clientService.disconnectClient(clientId).get();
        } catch (final InterruptedException | ExecutionException e) {
            throw e.getCause();
        }
    }

    @Test(expected = RateLimitExceededException.class)
    public void test_disconnect_client_prevent_lwt_limit_exceeded() throws Throwable {
        when(pluginServiceRateLimitService.rateLimitExceeded()).thenReturn(true);
        try {
            clientService.disconnectClient(clientId, true).get();
        } catch (final InterruptedException | ExecutionException e) {
            throw e.getCause();
        }
    }

    @Test(expected = RateLimitExceededException.class)
    public void test_invalidate_session_limit_exceeded() throws Throwable {
        when(pluginServiceRateLimitService.rateLimitExceeded()).thenReturn(true);
        try {
            clientService.invalidateSession(clientId).get();
        } catch (final InterruptedException | ExecutionException e) {
            throw e.getCause();
        }
    }


    @Test(expected = ExecutionException.class)
    public void test_disconnect_prevent_lwt_failed() throws Throwable {

        when(clientSessionPersistence.forceDisconnectClient(clientId, true, EXTENSION)).thenReturn(Futures.immediateFailedFuture(TestException.INSTANCE));
        clientService.disconnectClient(clientId, true).get();

    }

    @Test(expected = ExecutionException.class)
    public void test_disconnect_do_not_prevent_lwt_failed() throws Throwable {

        when(clientSessionPersistence.forceDisconnectClient(clientId, false, EXTENSION)).thenReturn(Futures.immediateFailedFuture(TestException.INSTANCE));
        clientService.disconnectClient(clientId).get();

    }

    @Test(expected = ExecutionException.class)
    public void test_invalidate_session_request_failed() throws Throwable {

        when(clientSessionPersistence.invalidateSession(clientId, EXTENSION)).thenReturn(Futures.immediateFailedFuture(TestException.INSTANCE));
        clientService.invalidateSession(clientId).get();

    }

    @Test(expected = NullPointerException.class)
    public void test_isClientConnected_client_id_null() {
        clientService.isClientConnected(null);
    }

    @Test(expected = NullPointerException.class)
    public void test_getSession_client_id_null() {
        clientService.getSession(null);
    }

    @Test(expected = NullPointerException.class)
    public void test_disconnectClient_client_id_null() {
        clientService.disconnectClient(null);
    }

    @Test(expected = NullPointerException.class)
    public void test_disconnectClient_prevent_client_id_null() {
        clientService.disconnectClient(null, false);
    }

    @Test(expected = NullPointerException.class)
    public void test_invalidateSession_client_id_null() {
        clientService.invalidateSession(null);
    }

    @Test(timeout = 20000)
    public void test_client_connected_null_success() throws Throwable {

        when(clientSessionPersistence.getSession(clientId)).thenReturn(null);
        assertFalse(clientService.isClientConnected(clientId).get());

    }

    @Test(timeout = 20000)
    public void test_get_session_null_success() throws Throwable {
        when(clientSessionPersistence.getSession(clientId)).thenReturn(null);
        assertEquals(Optional.empty(), clientService.getSession(clientId).get());
    }

    @Test(timeout = 20000)
    public void test_get_session_success() throws Throwable {

        final ClientSession session = getSession(true);

        when(clientSessionPersistence.getSession(clientId)).thenReturn(session);
        final Optional<SessionInformation> sessionInformation = clientService.getSession(clientId).get();

        assertTrue(sessionInformation.isPresent());
        assertEquals(true, sessionInformation.get().isConnected());
        assertEquals(clientId, sessionInformation.get().getClientIdentifier());
        assertEquals(sessionExpiry, sessionInformation.get().getSessionExpiryInterval());
    }

    @Test(timeout = 20000)
    public void test_disconnect_client_do_not_prevent_lwt_null_success() throws Throwable {
        when(clientSessionPersistence.forceDisconnectClient(clientId, false, EXTENSION)).thenReturn(Futures.immediateFuture(null));
        assertEquals(null, clientService.disconnectClient(clientId).get());
    }

    @Test(timeout = 20000)
    public void test_disconnect_client_do_not_prevent_lwt_true_success() throws Throwable {
        when(clientSessionPersistence.forceDisconnectClient(clientId, false, EXTENSION)).thenReturn(Futures.immediateFuture(true));
        assertEquals(true, clientService.disconnectClient(clientId).get());
    }

    @Test(timeout = 20000)
    public void test_disconnect_client_do_not_prevent_lwt_false_success() throws Throwable {
        when(clientSessionPersistence.forceDisconnectClient(clientId, false, EXTENSION)).thenReturn(Futures.immediateFuture(false));
        assertEquals(false, clientService.disconnectClient(clientId).get());
    }

    @Test(timeout = 20000)
    public void test_disconnect_client_prevent_lwt_null_success() throws Throwable {
        when(clientSessionPersistence.forceDisconnectClient(clientId, true, EXTENSION)).thenReturn(Futures.immediateFuture(null));
        assertEquals(null, clientService.disconnectClient(clientId, true).get());
    }

    @Test(timeout = 20000)
    public void test_disconnect_client_prevent_lwt_true_success() throws Throwable {
        when(clientSessionPersistence.forceDisconnectClient(clientId, true, EXTENSION)).thenReturn(Futures.immediateFuture(true));
        assertEquals(true, clientService.disconnectClient(clientId, true).get());
    }

    @Test(timeout = 20000)
    public void test_disconnect_client_prevent_lwt_false_success() throws Throwable {
        when(clientSessionPersistence.forceDisconnectClient(clientId, true, EXTENSION)).thenReturn(Futures.immediateFuture(false));
        assertEquals(false, clientService.disconnectClient(clientId, true).get());
    }

    @Test(timeout = 20000, expected = ExecutionException.class)
    public void test_invalidate_session_null_failed() throws Throwable {
        when(clientSessionPersistence.invalidateSession(clientId, EXTENSION)).thenReturn(Futures.immediateFuture(null));
        assertEquals(null, clientService.invalidateSession(clientId).get());
    }

    @Test(timeout = 20000)
    public void test_invalidate_session_true_success() throws Throwable {
        when(clientSessionPersistence.invalidateSession(clientId, EXTENSION)).thenReturn(Futures.immediateFuture(true));
        assertEquals(true, clientService.invalidateSession(clientId).get());
    }

    @Test(timeout = 20000)
    public void test_invalidate_session_false_success() throws Throwable {
        when(clientSessionPersistence.invalidateSession(clientId, EXTENSION)).thenReturn(Futures.immediateFuture(false));
        assertEquals(false, clientService.invalidateSession(clientId).get());
    }

    @Test(timeout = 20000)
    public void test_client_connected_true_success() throws Throwable {

        when(clientSessionPersistence.getSession(clientId)).thenReturn(getSession(true));
        assertEquals(true, clientService.isClientConnected(clientId).get());

    }

    @Test(timeout = 20000)
    public void test_client_connected_false_success() throws Throwable {

        when(clientSessionPersistence.getSession(clientId)).thenReturn(getSession(false));
        assertEquals(false, clientService.isClientConnected(clientId).get());

    }

    @NotNull
    private ClientSession getSession(final boolean connected) {
        return new ClientSession(connected, sessionExpiry, new ClientSessionWill(Mockito.mock(MqttWillPublish.class), 12345L));
    }
}