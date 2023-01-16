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

package com.hivemq.extensions.client.parameter;

import com.google.common.collect.ImmutableMap;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.services.exception.LimitExceededException;
import com.hivemq.mqtt.handler.publish.PublishFlushHandler;
import io.netty.channel.Channel;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.Test;
import util.TestChannelAttribute;

import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConnectionAttributesTest {

    private @NotNull ConnectionAttributes connectionAttributes;
    private @NotNull ClientConnection clientConnection;
    private @NotNull Channel channel;

    @Before
    public void setUp() {
        connectionAttributes = new ConnectionAttributes(1000);
        channel = mock(Channel.class);
        clientConnection = new ClientConnection(channel, mock(PublishFlushHandler.class));
        when(channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME)).thenReturn(new TestChannelAttribute<>(clientConnection));
    }

    @Test
    public void test_getInstanceIfPresent_present() {
        clientConnection.setConnectionAttributes(connectionAttributes);

        final ConnectionAttributes returnConnectionAttributes = ConnectionAttributes.getInstanceIfPresent(channel);

        assertEquals(connectionAttributes, returnConnectionAttributes);
    }

    @Test
    public void test_getInstanceIfPresent_not_present() {
        clientConnection.setConnectionAttributes(null);

        final ConnectionAttributes returnConnectionAttributes = ConnectionAttributes.getInstanceIfPresent(channel);

        assertNull(returnConnectionAttributes);
    }

    @Test
    public void test_getInstance_present() {
        clientConnection.setConnectionAttributes(connectionAttributes);

        final ConnectionAttributes returnConnectionAttributes = ConnectionAttributes.getInstance(channel);

        assertEquals(connectionAttributes, returnConnectionAttributes);
    }

    @Test
    public void test_getInstance_not_present() {
        clientConnection.setConnectionAttributes(null);

        final ConnectionAttributes returnConnectionAttributes = ConnectionAttributes.getInstance(channel);

        assertNotNull(returnConnectionAttributes);
    }

    @Test
    public void test_put() {
        final String key = "test.key";
        final ByteBuffer value = ByteBuffer.wrap("test.value".getBytes());

        connectionAttributes.put(key, value);

        // checks if test passes without exceptions
    }

    @Test
    public void test_put_present() {
        final String key = "test.key";
        final ByteBuffer value = ByteBuffer.wrap("test.value".getBytes());

        connectionAttributes.put(key, value);
        connectionAttributes.put(key, value);

        // checks if test passes without exceptions
    }

    @Test
    public void test_put_readOnly() {
        final String key = "test.key";
        final ByteBuffer value = ByteBuffer.wrap("test.value".getBytes());

        final ByteBuffer putValue = value.duplicate();
        connectionAttributes.put(key, putValue);

        byte i = putValue.get(0);
        i++;
        putValue.put(0, i);

        final Optional<ByteBuffer> getValue = connectionAttributes.get(key);

        assertTrue(getValue.isPresent());
        assertEquals(value, getValue.get());
    }

    @Test(expected = NullPointerException.class)
    @SuppressWarnings("ConstantConditions")
    public void test_put_null_key() {
        final String key = null;
        final ByteBuffer value = ByteBuffer.wrap("test.value".getBytes());

        connectionAttributes.put(key, value);
    }

    @Test(expected = NullPointerException.class)
    @SuppressWarnings("ConstantConditions")
    public void test_put_null_value() {
        final String key = "test.key";
        final ByteBuffer value = null;

        connectionAttributes.put(key, value);
    }

    @Test
    public void test_get() {
        final String key = "test.key";
        final ByteBuffer value = ByteBuffer.wrap("test.value".getBytes());

        connectionAttributes.put(key, value);

        final Optional<ByteBuffer> returnValue = connectionAttributes.get(key);

        assertTrue(returnValue.isPresent());
        assertEquals(value, returnValue.get());
    }

    @Test
    public void test_get_not_present() {
        final String key = "test.key";

        final Optional<ByteBuffer> returnValue = connectionAttributes.get(key);

        assertFalse(returnValue.isPresent());
    }

    @Test(expected = ReadOnlyBufferException.class)
    public void test_get_immutable() {
        final String key = "test.key";
        final ByteBuffer value = ByteBuffer.wrap("test.value".getBytes());

        connectionAttributes.put(key, value);

        final Optional<ByteBuffer> returnValue1 = connectionAttributes.get(key);
        assertTrue(returnValue1.isPresent());
        assertEquals(value, returnValue1.get());

        returnValue1.get().put(0, (byte) 10);

    }

    @Test(expected = NullPointerException.class)
    @SuppressWarnings("ConstantConditions")
    public void test_get_null_key() {
        final String key = null;

        connectionAttributes.get(key);
    }

    @Test
    public void test_getAll() {
        final ImmutableMap<String, ByteBuffer> values = ImmutableMap.of("test.key1",
                ByteBuffer.wrap("test.value1".getBytes()),
                "test.key2",
                ByteBuffer.wrap("test.value2".getBytes()));

        for (final Map.Entry<String, ByteBuffer> value : values.entrySet()) {
            connectionAttributes.put(value.getKey(), value.getValue());
        }

        final Optional<Map<String, ByteBuffer>> returnValues = connectionAttributes.getAll();

        assertTrue(returnValues.isPresent());
        assertAllEquals(values, returnValues.get());
    }

    @Test
    public void test_getAll_empty() {
        final Optional<Map<String, ByteBuffer>> returnValues = connectionAttributes.getAll();

        assertFalse(returnValues.isPresent());
    }

    @Test
    public void test_getAll_immutable() {
        final ImmutableMap<String, ByteBuffer> values = ImmutableMap.of("test.key1",
                ByteBuffer.wrap("test.value1".getBytes()),
                "test.key2",
                ByteBuffer.wrap("test.value2".getBytes()));

        for (final Map.Entry<String, ByteBuffer> value : values.entrySet()) {
            connectionAttributes.put(value.getKey(), value.getValue());
        }

        final Optional<Map<String, ByteBuffer>> returnValues1 = connectionAttributes.getAll();

        assertTrue(returnValues1.isPresent());
        assertAllEquals(values, returnValues1.get());

        final AtomicInteger exceptions = new AtomicInteger(0);

        for (final String key : values.keySet()) {
            try {
                returnValues1.get().get(key).put(0, (byte) 10);
            } catch (final ReadOnlyBufferException e) {
                exceptions.incrementAndGet();
            }
        }

        final Optional<Map<String, ByteBuffer>> returnValues2 = connectionAttributes.getAll();

        assertTrue(returnValues2.isPresent());
        assertAllEquals(values, returnValues2.get());
        assertEquals(values.size(), exceptions.get());
    }

    @Test
    public void test_remove() {
        final String key = "test.key";
        final ByteBuffer value = ByteBuffer.wrap("test.value".getBytes());

        connectionAttributes.put(key, value);

        final Optional<ByteBuffer> returnValue = connectionAttributes.remove(key);

        assertTrue(returnValue.isPresent());
        assertEquals(value, returnValue.get());
    }

    @Test
    public void test_remove_not_present() {
        final String key = "test.key";

        final Optional<ByteBuffer> returnValue = connectionAttributes.remove(key);

        assertFalse(returnValue.isPresent());
    }

    @Test(expected = NullPointerException.class)
    @SuppressWarnings("ConstantConditions")
    public void test_remove_null_key() {
        final String key = null;

        connectionAttributes.remove(key);
    }

    @Test
    public void test_clear() {
        final String key1 = "test.key1";
        final ByteBuffer value1 = ByteBuffer.wrap("test.value1".getBytes());
        final String key2 = "test.key2";
        final ByteBuffer value2 = ByteBuffer.wrap("test.value2".getBytes());

        connectionAttributes.put(key1, value1);
        connectionAttributes.put(key2, value2);

        connectionAttributes.clear();

        assertFalse(connectionAttributes.getAll().isPresent());
    }

    @Test
    public void test_clear_empty() {
        connectionAttributes.clear();

        // checks if test passes without exceptions
    }

    @Test(expected = LimitExceededException.class)
    public void test_limit_exceeded() {
        connectionAttributes.put("key", ByteBuffer.wrap(RandomUtils.nextBytes(1001)));
    }

    @Test
    public void test_limit_matched() {
        connectionAttributes.put("key", ByteBuffer.wrap(RandomUtils.nextBytes(1000)));

        final Optional<ByteBuffer> key = connectionAttributes.get("key");

        assertTrue(key.isPresent());
        assertEquals(1000, key.get().remaining());
    }

    @Test
    public void test_limit_smaller() {
        connectionAttributes.put("key", ByteBuffer.wrap(RandomUtils.nextBytes(999)));

        final Optional<ByteBuffer> key = connectionAttributes.get("key");

        assertTrue(key.isPresent());
        assertEquals(999, key.get().remaining());
    }

    static void assertAllEquals(final Map<String, ByteBuffer> expected, final Map<String, ByteBuffer> actual) {
        assertEquals(expected.keySet(), actual.keySet());
        for (final String key : expected.keySet()) {
            assertEquals(expected.get(key), expected.get(key));
        }
    }
}
