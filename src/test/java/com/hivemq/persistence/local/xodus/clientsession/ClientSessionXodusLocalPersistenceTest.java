/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.persistence.local.xodus.clientsession;

import com.codahale.metrics.Counter;
import com.google.common.collect.Lists;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.iteration.BucketChunkResult;
import com.hivemq.logging.EventLog;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.connect.MqttWillPublish;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.persistence.NoSessionException;
import com.hivemq.persistence.PersistenceEntry;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.clientsession.ClientSession;
import com.hivemq.persistence.clientsession.ClientSessionWill;
import com.hivemq.persistence.clientsession.PendingWillMessages;
import com.hivemq.persistence.exception.InvalidSessionExpiryIntervalException;
import com.hivemq.persistence.local.xodus.EnvironmentUtil;
import com.hivemq.persistence.local.xodus.bucket.BucketUtils;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.util.LocalPersistenceFileUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import util.TestBucketUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.SESSION_EXPIRE_ON_DISCONNECT;
import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.SESSION_EXPIRY_MAX;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Christoph Schäbel
 */
@SuppressWarnings("NullabilityAnnotations")
public class ClientSessionXodusLocalPersistenceTest {

    private static final int BUCKET_COUNT = 4;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private ClientSessionXodusLocalPersistence persistence;
    private PublishPayloadPersistence payloadPersistence;
    private EventLog eventLog;
    private PersistenceStartup persistenceStartup;

    @Before
    public void setUp() throws Exception {
        payloadPersistence = mock(PublishPayloadPersistence.class);
        eventLog = mock(EventLog.class);

        final LocalPersistenceFileUtil localPersistenceFileUtil = mock(LocalPersistenceFileUtil.class);

        InternalConfigurations.PERSISTENCE_CLOSE_RETRIES.set(3);
        InternalConfigurations.PERSISTENCE_CLOSE_RETRY_INTERVAL_MSEC.set(5);
        InternalConfigurations.PERSISTENCE_BUCKET_COUNT.set(BUCKET_COUNT);
        when(localPersistenceFileUtil.getVersionedLocalPersistenceFolder(anyString(), anyString())).thenReturn(
                temporaryFolder.newFolder());

        persistenceStartup = new PersistenceStartup();

        final MetricsHolder metricsHolder = mock(MetricsHolder.class);
        when(metricsHolder.getStoredWillMessagesCount()).thenReturn(mock(Counter.class));

        persistence = new ClientSessionXodusLocalPersistence(localPersistenceFileUtil,
                new EnvironmentUtil(),
                payloadPersistence,
                eventLog,
                persistenceStartup,
                metricsHolder);
        persistence.start();
    }

    @After
    public void tearDown() throws Exception {
        persistence.closeDB();
        persistenceStartup.finish();
    }

    @Test
    public void test_put_get() {
        persistence.put("clientId",
                new ClientSession(false, SESSION_EXPIRY_MAX),
                123L,
                BucketUtils.getBucket("clientId", BUCKET_COUNT));

        final ClientSession clientSession =
                persistence.getSession("clientId", BucketUtils.getBucket("clientId", BUCKET_COUNT));
        assertNotNull(clientSession);

        assertFalse(clientSession.isConnected());

        final ClientSession session = persistence.getSession("clientId");
        assertNotNull(session);

        assertEquals(123L, Objects.requireNonNull(persistence.getTimestamp("clientId")).longValue());
    }

    @Test
    public void test_getDisconnected() {
        final String client1 = TestBucketUtil.getId(1, BUCKET_COUNT);
        final String client2 = TestBucketUtil.getId(1, BUCKET_COUNT);

        persistence.put(client1, new ClientSession(true, SESSION_EXPIRY_MAX), 123L, 1);
        persistence.put(client2, new ClientSession(true, SESSION_EXPIRY_MAX), 123L, 1);

        persistence.disconnect(client2, 124L, false, 1, SESSION_EXPIRY_MAX);


        final Set<String> disconnectedClients = persistence.getDisconnectedClients(1);

        assertEquals(1, disconnectedClients.size());
        assertTrue(disconnectedClients.contains(client2));
    }

    @Test
    public void test_getDisconnectedClients() {
        final String client1 = TestBucketUtil.getId(1, BUCKET_COUNT);

        persistence.put(client1, new ClientSession(false, SESSION_EXPIRY_MAX), 123L, 1);

        final Set<String> disconnectedClients = persistence.getDisconnectedClients(1);

        assertEquals(1, disconnectedClients.size());
        assertTrue(disconnectedClients.contains(client1));
    }

    @Test
    public void test_getDisconnectedClients_single_instance_no_tombstone() {
        final String client1 = TestBucketUtil.getId(1, BUCKET_COUNT);
        final String client2 = TestBucketUtil.getId(1, BUCKET_COUNT);

        persistence.put(client1, new ClientSession(false, SESSION_EXPIRY_MAX), 123L, 1);
        persistence.put(client2, new ClientSession(false, SESSION_EXPIRE_ON_DISCONNECT), 123L, 1);

        final Set<String> disconnectedClients = persistence.getDisconnectedClients(1);

        assertEquals(1, disconnectedClients.size());
        assertTrue(disconnectedClients.contains(client1));
    }

    @Test
    public void test_getDisconnectedClients_ttl() {
        final String client1 = TestBucketUtil.getId(1, BUCKET_COUNT);
        final String client2 = TestBucketUtil.getId(1, BUCKET_COUNT);
        final String client3 = TestBucketUtil.getId(1, BUCKET_COUNT);

        persistence.put(client1, new ClientSession(false, SESSION_EXPIRY_MAX), System.currentTimeMillis() - 100000L, 1);
        persistence.put(client2, new ClientSession(false, 10), System.currentTimeMillis() - 100000L, 1);
        persistence.put(client3, new ClientSession(false, 1000000), System.currentTimeMillis(), 1);

        final Set<String> disconnectedClients = persistence.getDisconnectedClients(1);

        assertEquals(2, disconnectedClients.size());
        assertTrue(disconnectedClients.contains(client1));
        assertTrue(disconnectedClients.contains(client3));
    }

    @Test
    public void test_disconnect_right_node() {
        persistence.put("clientId",
                new ClientSession(true, SESSION_EXPIRY_MAX),
                123L,
                BucketUtils.getBucket("clientId", BUCKET_COUNT));

        persistence.disconnect("clientId",
                321L,
                false,
                BucketUtils.getBucket("clientId", BUCKET_COUNT),
                SESSION_EXPIRY_MAX);
        persistence.disconnect("clientId2",
                4321L,
                false,
                BucketUtils.getBucket("clientId2", BUCKET_COUNT),
                SESSION_EXPIRY_MAX);

        assertFalse(Objects.requireNonNull(persistence.getSession("clientId")).isConnected());
        assertEquals(321L, Objects.requireNonNull(persistence.getTimestamp("clientId")).longValue());

        assertFalse(Objects.requireNonNull(persistence.getSession("clientId2", false)).isConnected());
        assertEquals(4321L, Objects.requireNonNull(persistence.getTimestamp("clientId2")).longValue());
    }


    @Test
    public void test_removeWithTimestamp_single_client() {
        persistence.put("clientId",
                new ClientSession(false, SESSION_EXPIRY_MAX),
                123L,
                BucketUtils.getBucket("clientId", BUCKET_COUNT));
        persistence.removeWithTimestamp("clientId", BucketUtils.getBucket("clientId", BUCKET_COUNT));

        assertEquals(0, persistence.getSessionsCount());
        assertNull(persistence.getSession("clientId", BucketUtils.getBucket("clientId", BUCKET_COUNT)));
    }

    @Test
    public void test_clean_up_expired_sessions() {
        persistence.put("clientId1",
                new ClientSession(false, 10),
                System.currentTimeMillis() - 100000,
                BucketUtils.getBucket("clientId1", BUCKET_COUNT));
        final Set<String> expiredSessions = persistence.cleanUp(BucketUtils.getBucket("clientId1", BUCKET_COUNT));
        assertTrue(expiredSessions.contains("clientId1"));

        persistence.put("clientId2",
                new ClientSession(false, 100000),
                System.currentTimeMillis(),
                BucketUtils.getBucket("clientId2", BUCKET_COUNT));
        final Set<String> result2 = persistence.cleanUp(BucketUtils.getBucket("clientId2", BUCKET_COUNT));
        assertFalse(result2.contains("clientId2"));

        persistence.put("clientId3",
                new ClientSession(true, 10),
                System.currentTimeMillis() - 100000,
                BucketUtils.getBucket("clientId3", BUCKET_COUNT));
        final Set<String> result3 = persistence.cleanUp(BucketUtils.getBucket("clientId3", BUCKET_COUNT));
        assertFalse(result3.contains("clientId3"));

        verify(eventLog, times(1)).clientSessionExpired(anyLong(), anyString());
    }

    @Test
    public void test_clean_up_expired_sessions_twice() {
        persistence.put("clientId1",
                new ClientSession(false, 10),
                System.currentTimeMillis() - 10000,
                BucketUtils.getBucket("clientId1", BUCKET_COUNT));

        ClientSession expiredSession = persistence.getSession("clientId1");
        assertNull(expiredSession);

        final Set<String> result1 = persistence.cleanUp(BucketUtils.getBucket("clientId1", BUCKET_COUNT));
        assertTrue(result1.contains("clientId1"));

        expiredSession = persistence.getSession("clientId1");
        assertNull(expiredSession);

        final Set<String> result5 = persistence.cleanUp(BucketUtils.getBucket("clientId1", BUCKET_COUNT));
        assertTrue(result5.isEmpty());

        verify(eventLog, times(1)).clientSessionExpired(anyLong(), anyString());
    }

    @Test
    public void test_get_expired_session() {
        persistence.put("clientId1",
                new ClientSession(false, 10),
                System.currentTimeMillis() - 10000,
                BucketUtils.getBucket("clientId1", BUCKET_COUNT));

        final ClientSession expiredSession = persistence.getSession("clientId1");
        assertNull(expiredSession);
    }

    @Test
    public void test_get_expired_session_after_clean_up() {
        final MqttWillPublish mqttWillPublish = new MqttWillPublish.Mqtt3Builder().withTopic("topic")
                .withPayload("message".getBytes())
                .withQos(QoS.AT_LEAST_ONCE)
                .withRetain(true)
                .withHivemqId("hivemqId")
                .build();
        final ClientSession clientSession =
                new ClientSession(false, 10, new ClientSessionWill(mqttWillPublish, 1L), 123L);

        persistence.put("clientId1",
                clientSession,
                System.currentTimeMillis() - 10000,
                BucketUtils.getBucket("clientId1", BUCKET_COUNT));

        final Set<String> result1 = persistence.cleanUp(BucketUtils.getBucket("clientId1", BUCKET_COUNT));
        assertTrue(result1.contains("clientId1"));

        final ClientSession expiredSession = persistence.getSession("clientId1");
        assertNull(expiredSession);
    }

    @Test
    public void test_get_timestamp() {
        assertNull(persistence.getTimestamp("clientId", BucketUtils.getBucket("clientId", BUCKET_COUNT)));
        final long timestamp = 123L;
        persistence.put("clientId",
                new ClientSession(false, SESSION_EXPIRY_MAX),
                timestamp,
                BucketUtils.getBucket("clientId", BUCKET_COUNT));
        assertEquals(timestamp,
                Objects.requireNonNull(persistence.getTimestamp("clientId",
                        BucketUtils.getBucket("clientId", BUCKET_COUNT))).longValue());
    }

    @Test
    public void test_ttl() {
        final String clientId = "myClient";
        persistence.put(clientId,
                new ClientSession(false, SESSION_EXPIRY_MAX),
                123L,
                BucketUtils.getBucket(clientId, BUCKET_COUNT));
        final ClientSession clientSession =
                persistence.getSession(clientId, BucketUtils.getBucket(clientId, BUCKET_COUNT));
        assertNotNull(clientSession);
        assertEquals(clientSession.getSessionExpiryIntervalSec(), SESSION_EXPIRY_MAX);

        persistence.setSessionExpiryInterval(clientId, 12345, BucketUtils.getBucket(clientId, BUCKET_COUNT));
        final ClientSession updatedClientSession =
                persistence.getSession(clientId, BucketUtils.getBucket(clientId, BUCKET_COUNT));
        assertNotNull(updatedClientSession);
        assertEquals(12345, updatedClientSession.getSessionExpiryIntervalSec());
    }

    @Test(expected = NullPointerException.class)
    public void test_set_ttl_client_null() {
        //noinspection ConstantConditions
        persistence.setSessionExpiryInterval(null, 12345, BucketUtils.getBucket("clientId", BUCKET_COUNT));
    }

    @Test(expected = InvalidSessionExpiryIntervalException.class)
    public void test_invalid_ttl() {
        final String clientId = "myClient";

        persistence.put(clientId,
                new ClientSession(false, SESSION_EXPIRY_MAX),
                123L,
                BucketUtils.getBucket(clientId, BUCKET_COUNT));
        final ClientSession clientSession =
                persistence.getSession(clientId, BucketUtils.getBucket(clientId, BUCKET_COUNT));
        assertNotNull(clientSession);
        assertEquals(clientSession.getSessionExpiryIntervalSec(), SESSION_EXPIRY_MAX);

        persistence.setSessionExpiryInterval(clientId, -1, BucketUtils.getBucket(clientId, BUCKET_COUNT));
    }

    @Test(expected = InvalidSessionExpiryIntervalException.class)
    public void test_invalid_ttl_and_no_session() {
        final String clientId = "myClient";
        persistence.setSessionExpiryInterval(clientId, -1, BucketUtils.getBucket(clientId, BUCKET_COUNT));
    }

    @Test(expected = NoSessionException.class)
    public void test_set_ttl_no_session() {
        final String clientId = "myClient";
        persistence.setSessionExpiryInterval(clientId, 123, BucketUtils.getBucket(clientId, BUCKET_COUNT));
    }

    @Test(expected = NoSessionException.class)
    public void test_set_ttl_no_session_persisted_and_connected() {
        final String clientId = "myClient";
        persistence.put(clientId, new ClientSession(false, 0), 123L, BucketUtils.getBucket(clientId, BUCKET_COUNT));
        persistence.setSessionExpiryInterval(clientId, 123, BucketUtils.getBucket(clientId, BUCKET_COUNT));
    }

    @Test
    public void get_pending_wills() {
        final MqttWillPublish.Mqtt5Builder willPublish =
                new MqttWillPublish.Mqtt5Builder().withPayload("payload".getBytes())
                        .withTopic("topic")
                        .withQos(QoS.AT_MOST_ONCE)
                        .withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES)
                        .withHivemqId("hivemqId")
                        .withRetain(false)
                        .withDelayInterval(10);
        final ClientSessionWill sessionWill = new ClientSessionWill(willPublish.build(), 1L);
        persistence.put("noWill", new ClientSession(false, 0), System.currentTimeMillis(), 0);
        persistence.put("connected", new ClientSession(true, 0, sessionWill, 123L), System.currentTimeMillis(), 0);
        persistence.put("sendWill", new ClientSession(false, 0, sessionWill, 123L), System.currentTimeMillis(), 0);
        final Map<String, PendingWillMessages.PendingWill> wills = persistence.getPendingWills(0);

        assertEquals(1, wills.size());
        assertTrue(wills.containsKey("sendWill"));
    }

    @Test
    public void test_disconnected_no_will() {
        final String client1 = TestBucketUtil.getId(1, BUCKET_COUNT);

        persistence.put(client1,
                new ClientSession(true,
                        SESSION_EXPIRY_MAX,
                        new ClientSessionWill(new MqttWillPublish.Mqtt5Builder().withTopic("topic")
                                .withQos(QoS.AT_MOST_ONCE)
                                .withPayload("message".getBytes())
                                .withDelayInterval(0)
                                .withHivemqId("HiveMQId")
                                .withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES)
                                .build(), 1L),
                        234L),
                123L,
                1);

        final ClientSession clientSession = persistence.disconnect(client1, 124L, false, 1, 0L);

        assertNull(clientSession.getWillPublish());

        verify(payloadPersistence).decrementReferenceCounter(1L);
    }

    @Test
    public void test_disconnected_send_will() {
        when(payloadPersistence.get(anyLong())).thenReturn(new byte[]{});

        final String client1 = TestBucketUtil.getId(1, BUCKET_COUNT);

        persistence.put(client1,
                new ClientSession(true,
                        SESSION_EXPIRY_MAX,
                        new ClientSessionWill(new MqttWillPublish.Mqtt5Builder().withTopic("topic")
                                .withQos(QoS.AT_MOST_ONCE)
                                .withPayload("message".getBytes())
                                .withDelayInterval(0)
                                .withHivemqId("HiveMQId")
                                .withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES)
                                .build(), 1L),
                        234L),
                123L,
                1);

        final ClientSession clientSession = persistence.disconnect(client1, 124L, true, 1, 0L);

        assertNotNull(clientSession.getWillPublish());
        verify(payloadPersistence, never()).decrementReferenceCounter(1L);
    }

    @Test
    public void test_remove_will() {
        when(payloadPersistence.get(anyLong())).thenReturn(new byte[]{});
        final String client1 = TestBucketUtil.getId(1, BUCKET_COUNT);

        persistence.put(client1,
                new ClientSession(true,
                        SESSION_EXPIRY_MAX,
                        new ClientSessionWill(new MqttWillPublish.Mqtt5Builder().withTopic("topic")
                                .withQos(QoS.AT_MOST_ONCE)
                                .withPayload("message".getBytes())
                                .withDelayInterval(0)
                                .withHivemqId("HiveMQId")
                                .withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES)
                                .build(), 1L),
                        234L),
                123L,
                1);

        persistence.disconnect(client1, 124L, true, 1, 0L);
        final PersistenceEntry<ClientSession> entry = persistence.deleteWill(client1, 1);
        assertNotNull(entry);

        assertEquals(124L, entry.getTimestamp());
        assertNotNull(entry.getObject());
        verify(payloadPersistence).decrementReferenceCounter(1L);
    }

    @Test
    public void test_remove_will_connected() {
        final String client1 = TestBucketUtil.getId(1, BUCKET_COUNT);

        persistence.put(client1,
                new ClientSession(true,
                        SESSION_EXPIRY_MAX,
                        new ClientSessionWill(new MqttWillPublish.Mqtt5Builder().withTopic("topic")
                                .withQos(QoS.AT_MOST_ONCE)
                                .withPayload("message".getBytes())
                                .withDelayInterval(0)
                                .withHivemqId("HiveMQId")
                                .withUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES)
                                .build(), 1L),
                        234L),
                123L,
                1);

        final PersistenceEntry<ClientSession> entry = persistence.deleteWill(client1, 1);

        assertNull(entry);
        verify(payloadPersistence, never()).decrementReferenceCounter(1L);
    }

    @Test
    public void test_get_all_clients() {
        persistence.put("client1", new ClientSession(false, 0), 123L, 0);
        persistence.put("client2", new ClientSession(true, 0), 123L, 0);
        persistence.put("client3", new ClientSession(false, 1), 123L, 0);

        final Set<String> allClients = persistence.getAllClients(0);

        assertEquals(3, allClients.size());
        assertTrue(allClients.contains("client1"));
        assertTrue(allClients.contains("client2"));
        assertTrue(allClients.contains("client3"));
    }

    @Test
    public void test_graceful_handling_if_will_payload_is_missing() {
        final int bucketIndex = BucketUtils.getBucket("clientId", BUCKET_COUNT);
        final MqttWillPublish willPublish = new MqttWillPublish.Mqtt3Builder().withTopic("abc")
                .withPayload(new byte[]{})
                .withQos(QoS.EXACTLY_ONCE)
                .withHivemqId("hivemqId")
                .build();
        persistence.put("clientId",
                new ClientSession(true, 1000, new ClientSessionWill(willPublish, 123L), 234L),
                System.currentTimeMillis(),
                bucketIndex);

        final ClientSession session = persistence.getSession("clientId", bucketIndex);
        assertNotNull(session);
        assertNull(session.getWillPublish());
    }


    @Test(timeout = 10_000)
    public void test_get_chunk_match_some() {
        persistence.put("clientId", new ClientSession(true, 1000), 123L, 1);
        persistence.put("clientId2", new ClientSession(true, 1000), 123L, 1);

        final Map<String, ClientSession> client1Entries = persistence.getAllClientsChunk(1, null, 10).getValue();
        final Map<String, ClientSession> client2Entries = persistence.getAllClientsChunk(1, null, 10).getValue();

        assertNotNull(client1Entries.get("clientId"));
        assertNotNull(client1Entries.get("clientId2"));

        assertNotNull(client2Entries.get("clientId"));
        assertNotNull(client2Entries.get("clientId2"));
    }

    @Test(timeout = 10_000)
    public void test_get_chunk_many_clients() {
        for (int i = 0; i < 100; i++) {
            persistence.put("client-" + i, new ClientSession(true, 1000), 123L, 1);
        }

        final ArrayList<String> clientIds = Lists.newArrayList();
        BucketChunkResult<Map<String, ClientSession>> chunk = null;

        do {
            chunk = persistence.getAllClientsChunk(1, chunk != null ? chunk.getLastKey() : null, 16);
            clientIds.addAll(chunk.getValue().keySet());
        } while (!chunk.isFinished());

        final Set<String> seenIds = new HashSet<>();
        for (final String clientId : clientIds) {
            if (seenIds.contains(clientId)) {
                System.out.println(clientIds);
                fail("clientId " + clientId + " is duplicated. Total result count:" + clientIds.size());
            }
            seenIds.add(clientId);
        }

        assertEquals(100, clientIds.size());
    }

    @Test(timeout = 10_000)
    public void test_get_chunk_remove_last_key_between_iterations() {
        for (int i = 0; i < 100; i++) {
            persistence.put("client-" + i, new ClientSession(true, 1000), 123L, 1);
        }

        final ArrayList<String> clientIds = Lists.newArrayList();
        BucketChunkResult<Map<String, ClientSession>> chunk = null;

        do {
            if (chunk != null && chunk.getLastKey() != null) {
                persistence.removeWithTimestamp(chunk.getLastKey(), 1);
            }
            chunk = persistence.getAllClientsChunk(1, chunk != null ? chunk.getLastKey() : null, 1);
            clientIds.addAll(chunk.getValue().keySet());
        } while (!chunk.isFinished());

        final Set<String> seenIds = new HashSet<>();
        for (final String clientId : clientIds) {
            if (seenIds.contains(clientId)) {
                System.out.println(clientIds);
                fail("clientId " + clientId + " is duplicated. Total result count:" + clientIds.size());
            }
            seenIds.add(clientId);
        }

        assertEquals(100, clientIds.size());
    }

    @Test(timeout = 10_000)
    public void test_get_chunk_empty_between_iterations() {
        persistence.put("client1", new ClientSession(true, 1000), 123L, 1);
        persistence.put("client2", new ClientSession(true, 1000), 123L, 1);
        persistence.put("client3", new ClientSession(true, 1000), 123L, 1);

        final ArrayList<String> clientIds = Lists.newArrayList();
        BucketChunkResult<Map<String, ClientSession>> chunk = null;

        do {
            if (chunk != null && chunk.getLastKey() != null) {
                for (int i = 0; i < 100; i++) {
                    persistence.removeWithTimestamp("client1", 1);
                    persistence.removeWithTimestamp("client2", 1);
                    persistence.removeWithTimestamp("client3", 1);
                }
            }
            chunk = persistence.getAllClientsChunk(1, chunk != null ? chunk.getLastKey() : null, 1);
            clientIds.addAll(chunk.getValue().keySet());
        } while (!chunk.isFinished());


        assertEquals(1, clientIds.size());
    }

    @Test(timeout = 10_000)
    public void test_get_chunk_skip_expired_clients() {
        persistence.put("client1", new ClientSession(true, 1000), System.currentTimeMillis(), 1);
        persistence.put("client2", new ClientSession(false, 1000), System.currentTimeMillis(), 1);
        persistence.put("client3", new ClientSession(false, 1000), 123L, 1);
        persistence.put("client4", new ClientSession(true, 1000), 123L, 1);

        final ArrayList<String> clientIds = Lists.newArrayList();
        BucketChunkResult<Map<String, ClientSession>> chunk = null;

        do {
            chunk = persistence.getAllClientsChunk(1, chunk != null ? chunk.getLastKey() : null, 1);
            clientIds.addAll(chunk.getValue().keySet());
        } while (!chunk.isFinished());


        assertEquals(3, clientIds.size());

        assertFalse(clientIds.contains("client3"));
    }

    @Test(timeout = 10_000)
    public void test_get_chunk_only_expired_clients() {
        persistence.put("client1", new ClientSession(false, 1000), 123L, 1);
        persistence.put("client2", new ClientSession(false, 1000), 123L, 1);

        final ArrayList<String> clientIds = Lists.newArrayList();
        BucketChunkResult<Map<String, ClientSession>> chunk = null;

        do {
            chunk = persistence.getAllClientsChunk(1, chunk != null ? chunk.getLastKey() : null, 1);
            clientIds.addAll(chunk.getValue().keySet());
        } while (!chunk.isFinished());

        assertEquals(0, clientIds.size());
    }

    @Test(timeout = 30_000)
    public void test_get_chunk_many_clients_random_ids() {
        final ArrayList<String> clientIdList = getRandomUniqueIds();

        for (int i = 0; i < 100; i++) {
            persistence.put(clientIdList.get(i), new ClientSession(true, 1000), System.currentTimeMillis(), 1);
        }

        final ArrayList<String> clientIds = Lists.newArrayList();
        BucketChunkResult<Map<String, ClientSession>> chunk = null;

        do {
            chunk = persistence.getAllClientsChunk(1, chunk != null ? chunk.getLastKey() : null, 16);
            clientIds.addAll(chunk.getValue().keySet());
        } while (!chunk.isFinished());

        final Set<String> seenIds = new HashSet<>();
        for (final String clientId : clientIds) {
            if (seenIds.contains(clientId)) {
                System.out.println(clientIds);
                fail("clientId " + clientId + " is duplicated. Total result count:" + clientIds.size());
            }
            seenIds.add(clientId);
        }

        assertEquals(100, clientIds.size());
    }

    @Test(timeout = 10_000)
    public void test_queue_limit() {
        persistence.put("clientId", new ClientSession(true, 1000L, null, 10L), System.currentTimeMillis(), 0);

        final ClientSession session = persistence.getSession("clientId", 0);
        assertNotNull(session);

        assertEquals(10L, Objects.requireNonNull(session.getQueueLimit()).longValue());
    }

    @NotNull
    public ArrayList<String> getRandomUniqueIds() {
        final Set<String> clientIdSet = new HashSet<>();

        final Random random = new Random();
        while (clientIdSet.size() < 100) {
            clientIdSet.add(RandomStringUtils.randomAlphanumeric(random.nextInt(100)));
        }
        return new ArrayList<>(clientIdSet);
    }
}
