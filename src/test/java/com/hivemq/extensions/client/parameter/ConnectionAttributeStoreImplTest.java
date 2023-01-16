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
import com.hivemq.extension.sdk.api.client.parameter.ConnectionAttributeStore;
import com.hivemq.mqtt.handler.publish.PublishFlushHandler;
import io.netty.channel.Channel;
import io.netty.util.Attribute;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @since 4.0.0
 */
public class ConnectionAttributeStoreImplTest {

    private @NotNull Channel channel;
    private @NotNull ConnectionAttributes connectionAttributes;
    private @NotNull ConnectionAttributeStore connectionAttributeStore;
    private @NotNull ClientConnection clientConnection;

    @Before
    public void setUp() {
        channel = mock(Channel.class);

        connectionAttributes = new ConnectionAttributes(1000);
        connectionAttributeStore = new ConnectionAttributeStoreImpl(channel);

        clientConnection = new ClientConnection(channel, mock(PublishFlushHandler.class));

        //noinspection unchecked
        final Attribute<ClientConnection> clientConnectionAttribute = mock(Attribute.class);
        when(channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME)).thenReturn(clientConnectionAttribute);
        when(clientConnectionAttribute.get()).thenReturn(clientConnection);
    }

    @Test
    public void test_put_get() {
        final String key = "test.key";
        final ByteBuffer value = ByteBuffer.wrap("test.value".getBytes());

        clientConnection.setConnectionAttributes(connectionAttributes);

        connectionAttributeStore.put(key, value);

        final Optional<ByteBuffer> setValue = connectionAttributes.get(key);
        assertTrue(setValue.isPresent());
        assertEquals(value, setValue.get());
    }

    @Test
    public void test_put_present() {
        final String key = "test.key";
        final ByteBuffer value1 = ByteBuffer.wrap("test.value1".getBytes());
        final ByteBuffer value2 = ByteBuffer.wrap("test.value2".getBytes());

        clientConnection.setConnectionAttributes(connectionAttributes);

        connectionAttributeStore.put(key, value1);
        connectionAttributeStore.put(key, value2);

        final Optional<ByteBuffer> setValue = connectionAttributes.get(key);
        assertTrue(setValue.isPresent());
        assertEquals(value2, setValue.get());
    }

    @Test
    public void test_put_attribute_not_present() {
        final String key = "test.key";
        final ByteBuffer value = ByteBuffer.wrap("test.value".getBytes());

        clientConnection.setConnectionAttributes(null);

        connectionAttributeStore.put(key, value);
        assertNotNull(clientConnection.getConnectionAttributes());

        final Optional<ByteBuffer> setValue = clientConnection.getConnectionAttributes().get(key);
        assertTrue(setValue.isPresent());
        assertEquals(value, setValue.get());
    }

    @Test(expected = NullPointerException.class)
    @SuppressWarnings("ConstantConditions")
    public void test_put_null_key() {
        final String key = null;
        final ByteBuffer value = ByteBuffer.wrap("test.value".getBytes());

        connectionAttributeStore.put(key, value);
    }

    @Test(expected = NullPointerException.class)
    @SuppressWarnings("ConstantConditions")
    public void test_put_null_value() {
        final String key = "test.key";
        final ByteBuffer value = null;

        connectionAttributeStore.put(key, value);
    }

    @Test
    public void test_putAsString() {
        final String key = "test.key";
        final String value = "test.value";

        clientConnection.setConnectionAttributes(connectionAttributes);

        connectionAttributeStore.putAsString(key, value);

        final Optional<ByteBuffer> setValue = connectionAttributes.get(key);
        assertTrue(setValue.isPresent());
        assertEquals(ByteBuffer.wrap(value.getBytes()), setValue.get());
    }

    @Test
    public void test_putAsString_present() {
        final String key = "test.key";
        final String value1 = "test.value1";
        final String value2 = "test.value2";

        clientConnection.setConnectionAttributes(connectionAttributes);

        connectionAttributeStore.putAsString(key, value1);
        connectionAttributeStore.putAsString(key, value2);

        final Optional<ByteBuffer> setValue = connectionAttributes.get(key);
        assertTrue(setValue.isPresent());
        assertEquals(ByteBuffer.wrap(value2.getBytes()), setValue.get());
    }

    @Test
    public void test_putAsString_attribute_not_present() {
        final String key = "test.key";
        final String value = "test.value";

        clientConnection.setConnectionAttributes(null);

        connectionAttributeStore.putAsString(key, value);
        assertNotNull(clientConnection.getConnectionAttributes());

        final Optional<ByteBuffer> setValue = clientConnection.getConnectionAttributes().get(key);
        assertTrue(setValue.isPresent());
        assertEquals(ByteBuffer.wrap(value.getBytes()), setValue.get());
    }

    @Test(expected = NullPointerException.class)
    @SuppressWarnings("ConstantConditions")
    public void test_putAsString_null_key() {
        final String key = null;
        final String value = "test.value";

        connectionAttributeStore.putAsString(key, value);
    }

    @Test(expected = NullPointerException.class)
    @SuppressWarnings("ConstantConditions")
    public void test_putAsString_null_value() {
        final String key = "test.key";
        final String value = null;

        connectionAttributeStore.putAsString(key, value);
    }

    @Test
    public void test_putAsString_charset() {
        final String key = "test.key";
        final String value = "test.value";
        final Charset charset = StandardCharsets.ISO_8859_1;

        clientConnection.setConnectionAttributes(connectionAttributes);

        connectionAttributeStore.putAsString(key, value, charset);

        final Optional<ByteBuffer> setValue = connectionAttributes.get(key);
        assertTrue(setValue.isPresent());
        assertEquals(ByteBuffer.wrap(value.getBytes(charset)), setValue.get());
    }

    @Test
    public void test_putAsString_charset_present() {
        final String key = "test.key";
        final String value1 = "test.value1";
        final String value2 = "test.value2";
        final Charset charset = StandardCharsets.ISO_8859_1;

        clientConnection.setConnectionAttributes(connectionAttributes);

        connectionAttributeStore.putAsString(key, value1, charset);
        connectionAttributeStore.putAsString(key, value2, charset);

        final Optional<ByteBuffer> setValue = connectionAttributes.get(key);
        assertTrue(setValue.isPresent());
        assertEquals(ByteBuffer.wrap(value2.getBytes(charset)), setValue.get());
    }

    @Test
    public void test_putAsString_charset_attribute_not_present() {
        final String key = "test.key";
        final String value = "test.value";
        final Charset charset = StandardCharsets.ISO_8859_1;

        clientConnection.setConnectionAttributes(null);

        connectionAttributeStore.putAsString(key, value, charset);
        assertNotNull(clientConnection.getConnectionAttributes());

        final Optional<ByteBuffer> setValue = clientConnection.getConnectionAttributes().get(key);
        assertTrue(setValue.isPresent());
        assertEquals(ByteBuffer.wrap(value.getBytes(charset)), setValue.get());
    }

    @Test(expected = NullPointerException.class)
    @SuppressWarnings("ConstantConditions")
    public void test_putAsString_charset_null_key() {
        final String key = null;
        final String value = "test.value";
        final Charset charset = StandardCharsets.ISO_8859_1;

        connectionAttributeStore.putAsString(key, value, charset);
    }

    @Test(expected = NullPointerException.class)
    @SuppressWarnings("ConstantConditions")
    public void test_putAsString_charset_null_value() {
        final String key = "test.key";
        final String value = null;
        final Charset charset = StandardCharsets.ISO_8859_1;

        connectionAttributeStore.putAsString(key, value, charset);
    }

    @Test(expected = NullPointerException.class)
    @SuppressWarnings("ConstantConditions")
    public void test_putAsString_charset_null_charset() {
        final String key = "test.key";
        final String value = "test.value";
        final Charset charset = null;

        connectionAttributeStore.putAsString(key, value, charset);
    }

    @Test
    public void test_get_not_present() {
        final String key = "test.key";

        clientConnection.setConnectionAttributes(connectionAttributes);

        final Optional<ByteBuffer> returnValue = connectionAttributeStore.get(key);

        assertFalse(returnValue.isPresent());
    }

    @Test
    public void test_get_attribute_not_present() {
        final String key = "test.key";

        clientConnection.setConnectionAttributes(null);

        final Optional<ByteBuffer> returnValue = connectionAttributeStore.get(key);

        assertFalse(returnValue.isPresent());
    }

    @Test(expected = NullPointerException.class)
    @SuppressWarnings("ConstantConditions")
    public void test_get_null_key() {
        final String key = null;

        connectionAttributeStore.get(key);
    }

    @Test
    public void test_getAsString() {
        final String key = "test.key";
        final String value = "test.value";

        clientConnection.setConnectionAttributes(connectionAttributes);

        connectionAttributes.put(key, ByteBuffer.wrap(value.getBytes()));

        final Optional<String> returnValue = connectionAttributeStore.getAsString(key);

        assertTrue(returnValue.isPresent());
        assertEquals(value, returnValue.get());
    }

    @Test
    public void test_getAsString_not_present() {
        final String key = "test.key";

        clientConnection.setConnectionAttributes(connectionAttributes);

        final Optional<String> returnValue = connectionAttributeStore.getAsString(key);

        assertFalse(returnValue.isPresent());
    }

    @Test
    public void test_getAsString_attribute_not_present() {
        final String key = "test.key";

        clientConnection.setConnectionAttributes(null);

        final Optional<String> returnValue = connectionAttributeStore.getAsString(key);

        assertFalse(returnValue.isPresent());
    }

    @Test(expected = NullPointerException.class)
    @SuppressWarnings("ConstantConditions")
    public void test_getAsString_null_key() {
        final String key = null;

        connectionAttributeStore.getAsString(key);
    }

    @Test
    public void test_getAsString_charset() {
        final String key = "test.key";
        final String value = "test.value";
        final Charset charset = StandardCharsets.ISO_8859_1;

        clientConnection.setConnectionAttributes(connectionAttributes);

        connectionAttributes.put(key, ByteBuffer.wrap(value.getBytes(charset)));

        final Optional<String> returnValue = connectionAttributeStore.getAsString(key, charset);

        assertTrue(returnValue.isPresent());
        assertEquals(value, returnValue.get());
    }

    @Test
    public void test_getAsString_charset_not_present() {
        final String key = "test.key";
        final Charset charset = StandardCharsets.ISO_8859_1;

        clientConnection.setConnectionAttributes(connectionAttributes);

        final Optional<String> returnValue = connectionAttributeStore.getAsString(key, charset);

        assertFalse(returnValue.isPresent());
    }

    @Test
    public void test_getAsString_charset_attribute_not_present() {
        final String key = "test.key";
        final Charset charset = StandardCharsets.ISO_8859_1;

        clientConnection.setConnectionAttributes(null);

        final Optional<String> returnValue = connectionAttributeStore.getAsString(key, charset);

        assertFalse(returnValue.isPresent());
    }

    @Test(expected = NullPointerException.class)
    @SuppressWarnings("ConstantConditions")
    public void test_getAsString_charset_null_key() {
        final String key = null;
        final Charset charset = StandardCharsets.ISO_8859_1;

        connectionAttributeStore.getAsString(key, charset);
    }

    @Test(expected = NullPointerException.class)
    @SuppressWarnings("ConstantConditions")
    public void test_getAsString_charset_null_charset() {
        final String key = "test.key";
        final Charset charset = null;

        connectionAttributeStore.getAsString(key, charset);
    }

    @Test
    public void test_getAll() {
        final ImmutableMap<String, ByteBuffer> values = ImmutableMap.of("test.key1",
                ByteBuffer.wrap("test.value1".getBytes()),
                "test.key2",
                ByteBuffer.wrap("test.value2".getBytes()));

        clientConnection.setConnectionAttributes(connectionAttributes);

        for (final Map.Entry<String, ByteBuffer> entry : values.entrySet()) {
            connectionAttributeStore.put(entry.getKey(), entry.getValue());
        }

        final Optional<Map<String, ByteBuffer>> returnValues = connectionAttributeStore.getAll();

        assertTrue(returnValues.isPresent());
        ConnectionAttributesTest.assertAllEquals(values, returnValues.get());
    }

    @Test
    public void test_getAll_attribute_not_present() {
        clientConnection.setConnectionAttributes(null);

        final Optional<Map<String, ByteBuffer>> returnValues = connectionAttributeStore.getAll();

        assertFalse(returnValues.isPresent());
    }

    @Test
    public void test_remove() {
        final String key = "test.key";
        final ByteBuffer value = ByteBuffer.wrap("test.value".getBytes());

        clientConnection.setConnectionAttributes(connectionAttributes);

        connectionAttributes.put(key, value);
        connectionAttributes.put("test.key2", ByteBuffer.wrap("test.value2".getBytes()));

        final Optional<ByteBuffer> returnValue = connectionAttributeStore.remove(key);

        assertTrue(returnValue.isPresent());
        assertEquals(value, returnValue.get());
    }

    @Test
    public void test_remove_last() {
        final String key = "test.key";
        final ByteBuffer value = ByteBuffer.wrap("test.value".getBytes());

        clientConnection.setConnectionAttributes(connectionAttributes);

        connectionAttributes.put(key, value);

        final Optional<ByteBuffer> returnValue = connectionAttributeStore.remove(key);

        assertTrue(returnValue.isPresent());
        assertEquals(value, returnValue.get());
    }

    @Test
    public void test_remove_not_present() {
        final String key = "test.key";

        clientConnection.setConnectionAttributes(connectionAttributes);

        final Optional<ByteBuffer> returnValue = connectionAttributeStore.remove(key);

        assertFalse(returnValue.isPresent());
    }

    @Test
    public void test_remove_attribute_not_present() {
        final String key = "test.key";

        clientConnection.setConnectionAttributes(null);

        final Optional<ByteBuffer> returnValue = connectionAttributeStore.remove(key);

        assertFalse(returnValue.isPresent());
    }

    @Test(expected = NullPointerException.class)
    @SuppressWarnings("ConstantConditions")
    public void test_remove_null_key() {
        final String key = null;

        connectionAttributeStore.remove(key);
    }

    @Test
    public void test_clear() {
        final String key1 = "test.key1";
        final String key2 = "test.key2";
        final ByteBuffer value1 = ByteBuffer.wrap("test.value1".getBytes());
        final ByteBuffer value2 = ByteBuffer.wrap("test.value2".getBytes());

        clientConnection.setConnectionAttributes(connectionAttributes);

        connectionAttributeStore.put(key1, value1);
        connectionAttributeStore.put(key2, value2);

        connectionAttributeStore.clear();

        assertFalse(connectionAttributeStore.getAll().isPresent());
    }

    @Test
    public void test_clear_not_present() {
        clientConnection.setConnectionAttributes(connectionAttributes);

        connectionAttributeStore.clear();

        // checks if tests passes without exceptions
    }

    @Test
    public void test_clear_attribute_not_present() {
        clientConnection.setConnectionAttributes(null);

        connectionAttributeStore.clear();

        // checks if tests passes without exceptions
    }

    @Test
    public void test_concurrent_put_get_getAll_remove_clear() throws InterruptedException {
        final int THREADS = 100;
        final int EXECUTIONS = 100;

        clientConnection.setConnectionAttributes(connectionAttributes);

        final Random random = new Random();

        final ExceptionCountRunnable[] putRunnables = new ExceptionCountRunnable[THREADS];
        final ExceptionCountRunnable[] getRunnables = new ExceptionCountRunnable[THREADS];
        final ExceptionCountRunnable[] getAllRunnables = new ExceptionCountRunnable[THREADS];
        final ExceptionCountRunnable[] removeRunnables = new ExceptionCountRunnable[THREADS];
        final ExceptionCountRunnable[] clearRunnables = new ExceptionCountRunnable[THREADS];

        final Thread[] putThreads = new Thread[THREADS];
        final Thread[] getThreads = new Thread[THREADS];
        final Thread[] getAllThread = new Thread[THREADS];
        final Thread[] removeThreads = new Thread[THREADS];
        final Thread[] clearThreads = new Thread[THREADS];

        final ConnectionAttributeStore[] connectionAttributeStores = new ConnectionAttributeStore[THREADS];

        for (int i = 0; i < THREADS; i++) {
            final ConnectionAttributeStore connectionAttributeStore = new ConnectionAttributeStoreImpl(channel);
            connectionAttributeStores[i] = connectionAttributeStore;

            putRunnables[i] = new ExceptionCountRunnable(EXECUTIONS) {
                @Override
                public void runCount() {
                    connectionAttributeStore.putAsString(
                            "test.key" + random.nextInt(EXECUTIONS / 10),
                            RandomStringUtils.random(10));
                }
            };
            getRunnables[i] = new ExceptionCountRunnable(EXECUTIONS) {
                @Override
                public void runCount() {
                    connectionAttributeStore.getAsString("test.key" + random.nextInt(EXECUTIONS / 10));
                }
            };
            getAllRunnables[i] = new ExceptionCountRunnable(EXECUTIONS) {
                @Override
                public void runCount() {
                    connectionAttributeStore.getAll();
                }
            };
            removeRunnables[i] = new ExceptionCountRunnable(EXECUTIONS) {
                @Override
                public void runCount() {
                    connectionAttributeStore.remove("test.key" + random.nextInt(EXECUTIONS / 10));
                }
            };
            clearRunnables[i] = new ExceptionCountRunnable(EXECUTIONS) {
                @Override
                public void runCount() {
                    connectionAttributeStore.clear();
                }
            };
        }

        for (int i = 0; i < THREADS; i++) {
            putThreads[i] = new Thread(putRunnables[i]);
            getThreads[i] = new Thread(getRunnables[i]);
            getAllThread[i] = new Thread(getAllRunnables[i]);
            removeThreads[i] = new Thread(removeRunnables[i]);
            clearThreads[i] = new Thread(clearRunnables[i]);
        }

        for (int i = 0; i < THREADS; i++) {
            putThreads[i].start();
            getThreads[i].start();
            getAllThread[i].start();
            removeThreads[i].start();
            clearThreads[i].start();
        }

        for (int i = 0; i < THREADS; i++) {
            putThreads[i].join();
            getThreads[i].join();
            getAllThread[i].join();
            removeThreads[i].join();
            clearThreads[i].join();
        }

        for (int i = 0; i < THREADS; i++) {
            assertEquals(0, putRunnables[i].getExceptionCount());
            assertEquals(0, getRunnables[i].getExceptionCount());
            assertEquals(0, getAllRunnables[i].getExceptionCount());
            assertEquals(0, removeRunnables[i].getExceptionCount());
            assertEquals(0, clearRunnables[i].getExceptionCount());
        }

        final Optional<Map<String, ByteBuffer>> expected = connectionAttributes.getAll();
        for (int i = 0; i < THREADS; i++) {
            final Optional<Map<String, ByteBuffer>> actual = connectionAttributeStores[i].getAll();
            assertEquals(expected.isPresent(), actual.isPresent());
            if (expected.isPresent() && actual.isPresent()) {
                ConnectionAttributesTest.assertAllEquals(expected.get(), actual.get());
            }
        }
    }

    private static abstract class ExceptionCountRunnable implements Runnable {

        private final AtomicInteger exceptionCount = new AtomicInteger();
        private final int runCount;

        ExceptionCountRunnable(final int runCount) {
            this.runCount = runCount;
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < runCount; i++) {
                    runCount();
                }
            } catch (final Throwable throwable) {
                throwable.printStackTrace();
                exceptionCount.incrementAndGet();
            }
        }

        abstract void runCount();

        int getExceptionCount() {
            return exceptionCount.get();
        }
    }
}
