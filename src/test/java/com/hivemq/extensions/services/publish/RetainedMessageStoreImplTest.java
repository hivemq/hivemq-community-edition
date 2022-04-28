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
package com.hivemq.extensions.services.publish;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extension.sdk.api.packets.publish.PayloadFormatIndicator;
import com.hivemq.extension.sdk.api.services.exception.DoNotImplementException;
import com.hivemq.extension.sdk.api.services.exception.RateLimitExceededException;
import com.hivemq.extension.sdk.api.services.publish.RetainedMessageStore;
import com.hivemq.extension.sdk.api.services.publish.RetainedPublish;
import com.hivemq.extensions.iteration.AsyncIteratorFactory;
import com.hivemq.extensions.packets.general.UserPropertiesImpl;
import com.hivemq.extensions.services.PluginServiceRateLimitService;
import com.hivemq.extensions.services.executor.GlobalManagedExtensionExecutorService;
import com.hivemq.persistence.RetainedMessage;
import com.hivemq.persistence.retained.RetainedMessagePersistence;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import util.TestException;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class RetainedMessageStoreImplTest {

    private AutoCloseable closeableMock;

    private RetainedMessageStore retainedMessageStore;

    @Mock
    private RetainedMessagePersistence retainedMessagePersistence;

    @Mock
    private PluginServiceRateLimitService pluginServiceRateLimitService;

    @Mock
    private AsyncIteratorFactory asyncIteratorFactory;

    private GlobalManagedExtensionExecutorService managedPluginExecutorService;

    @Before
    public void setUp() throws Exception {
        closeableMock = MockitoAnnotations.openMocks(this);
        managedPluginExecutorService = new GlobalManagedExtensionExecutorService(Mockito.mock(ShutdownHooks.class));
        managedPluginExecutorService.postConstruct();
        retainedMessageStore = new RetainedMessageStoreImpl(retainedMessagePersistence, managedPluginExecutorService, pluginServiceRateLimitService, asyncIteratorFactory);
        when(pluginServiceRateLimitService.rateLimitExceeded()).thenReturn(false);
    }

    @After
    public void tearDown() throws Exception {
        managedPluginExecutorService.shutdown();
        closeableMock.close();
    }

    @Test(expected = RateLimitExceededException.class)
    public void test_get_retained_message_rate_limit_exceeded() throws Throwable {

        when(pluginServiceRateLimitService.rateLimitExceeded()).thenReturn(true);
        try {
            retainedMessageStore.getRetainedMessage("topic").get();
        } catch (final InterruptedException | ExecutionException e) {
            throw e.getCause();
        }

    }

    @Test(expected = RateLimitExceededException.class)
    public void test_clear_rate_limit_exceeded() throws Throwable {

        when(pluginServiceRateLimitService.rateLimitExceeded()).thenReturn(true);
        try {
            retainedMessageStore.clear().get();
        } catch (final InterruptedException | ExecutionException e) {
            throw e.getCause();
        }

    }

    @Test(expected = RateLimitExceededException.class)
    public void test_remove_rate_limit_exceeded() throws Throwable {

        when(pluginServiceRateLimitService.rateLimitExceeded()).thenReturn(true);
        try {
            retainedMessageStore.remove("topic").get();
        } catch (final InterruptedException | ExecutionException e) {
            throw e.getCause();
        }

    }

    @Test(expected = RateLimitExceededException.class)
    public void test_add_or_replace_rate_limit_exceeded() throws Throwable {

        when(pluginServiceRateLimitService.rateLimitExceeded()).thenReturn(true);
        try {
            retainedMessageStore.addOrReplace(Mockito.mock(RetainedPublish.class)).get();
        } catch (final InterruptedException | ExecutionException e) {
            throw e.getCause();
        }

    }

    @Test(expected = DoNotImplementException.class)
    public void test_add_or_replace_bad_impl() throws Throwable {

        try {
            retainedMessageStore.addOrReplace(new TestRetainedPublish()).get();
        } catch (final InterruptedException | ExecutionException e) {
            throw e.getCause();
        }

    }

    @Test(expected = NullPointerException.class)
    public void test_get_retained_message_null() {
        retainedMessageStore.getRetainedMessage(null);
    }

    @Test(expected = NullPointerException.class)
    public void test_remove_null() {
        retainedMessageStore.remove(null);
    }

    @Test(expected = NullPointerException.class)
    public void test_add_or_replace_null() {
        retainedMessageStore.addOrReplace(null);
    }

    @Test
    public void test_get_retained_message_success() throws ExecutionException, InterruptedException {

        when(retainedMessagePersistence.get("topic")).thenReturn(Futures.immediateFuture(getRetainedMessage()));

        final Optional<RetainedPublish> retainedPublish = retainedMessageStore.getRetainedMessage("topic").get();

        assertTrue(retainedPublish.isPresent());
        assertEquals(Qos.AT_LEAST_ONCE, retainedPublish.get().getQos());
        assertEquals(true, retainedPublish.get().getRetain());
        assertEquals("topic", retainedPublish.get().getTopic());
        assertTrue(retainedPublish.get().getPayloadFormatIndicator().isPresent());
        assertEquals(PayloadFormatIndicator.UTF_8, retainedPublish.get().getPayloadFormatIndicator().get());
        assertEquals(true, retainedPublish.get().getMessageExpiryInterval().isPresent());
        assertEquals(12345L, retainedPublish.get().getMessageExpiryInterval().get().longValue());
        assertEquals(true, retainedPublish.get().getResponseTopic().isPresent());
        assertEquals("response_topic", retainedPublish.get().getResponseTopic().get());
        assertEquals(true, retainedPublish.get().getContentType().isPresent());
        assertEquals("content_type", retainedPublish.get().getContentType().get());
        assertEquals(true, retainedPublish.get().getCorrelationData().isPresent());
        assertEquals(true, retainedPublish.get().getPayload().isPresent());
        assertEquals(true, retainedPublish.get().getUserProperties().asList().isEmpty());

    }

    @Test
    public void test_remove_success() throws InterruptedException {

        when(retainedMessagePersistence.remove(eq("topic"))).thenReturn(Futures.immediateFuture(null));

        final CompletableFuture<Void> remove = retainedMessageStore.remove("topic");

        assertNotNull(remove);
        while (!remove.isDone()) {
            Thread.sleep(10);
        }
        assertTrue(remove.isDone());
        assertFalse(remove.isCompletedExceptionally());

    }

    @Test
    public void test_clear_success() throws InterruptedException {

        when(retainedMessagePersistence.clear()).thenReturn(Futures.immediateFuture(null));

        final CompletableFuture<Void> clear = retainedMessageStore.clear();

        assertNotNull(clear);
        while (!clear.isDone()) {
            Thread.sleep(10);
        }
        assertTrue(clear.isDone());
        assertFalse(clear.isCompletedExceptionally());

    }

    @Test
    public void test_add_or_replace_success() throws InterruptedException {

        when(retainedMessagePersistence.persist(eq("topic"), any(RetainedMessage.class))).thenReturn(Futures.immediateFuture(null));

        final CompletableFuture<Void> addOrReplace = retainedMessageStore.addOrReplace(getRetainedPublish());

        assertNotNull(addOrReplace);
        while (!addOrReplace.isDone()) {
            Thread.sleep(10);
        }
        assertTrue(addOrReplace.isDone());
        assertFalse(addOrReplace.isCompletedExceptionally());

    }

    @Test(expected = TestException.class)
    public void test_get_retained_message_failed() throws Throwable {

        when(retainedMessagePersistence.get("topic")).thenReturn(Futures.immediateFailedFuture(TestException.INSTANCE));

        try {
            retainedMessageStore.getRetainedMessage("topic").get();
        } catch (final InterruptedException | ExecutionException e) {
            throw e.getCause();
        }

    }

    @Test(expected = TestException.class)
    public void test_remove_failed() throws Throwable {

        when(retainedMessagePersistence.remove(eq("topic"))).thenReturn(Futures.immediateFailedFuture(TestException.INSTANCE));

        try {
            retainedMessageStore.remove("topic").get();
        } catch (final InterruptedException | ExecutionException e) {
            throw e.getCause();
        }

    }

    @Test(expected = TestException.class)
    public void test_clear_failed() throws Throwable {

        when(retainedMessagePersistence.clear()).thenReturn(Futures.immediateFailedFuture(TestException.INSTANCE));

        try {
            retainedMessageStore.clear().get();
        } catch (final InterruptedException | ExecutionException e) {
            throw e.getCause();
        }

    }

    @Test(expected = TestException.class)
    public void test_add_or_replace_retained_message_failed() throws Throwable {

        when(retainedMessagePersistence.persist(anyString(), any(RetainedMessage.class))).thenReturn(
                Futures.immediateFailedFuture(TestException.INSTANCE));

        try {
            retainedMessageStore.addOrReplace(getRetainedPublish()).get();
        } catch (final InterruptedException | ExecutionException e) {
            throw e.getCause();
        }

    }

    private RetainedPublishImpl getRetainedPublish() {
        return new RetainedPublishImpl(
                Qos.AT_LEAST_ONCE, "topic", PayloadFormatIndicator.UTF_8, 12345L, "response_topic",
                ByteBuffer.wrap("correlation_data".getBytes()), "content_type", ByteBuffer.wrap("test3".getBytes()),
                UserPropertiesImpl.of(ImmutableList.of()));
    }

    private RetainedMessage getRetainedMessage() {
        return RetainedPublishImpl.convert(getRetainedPublish());
    }

    @SuppressWarnings("NullabilityAnnotations")
    private static class TestRetainedPublish implements RetainedPublish {

        @Override
        public Qos getQos() {
            return null;
        }

        @Override
        public String getTopic() {
            return null;
        }

        @Override
        public Optional<PayloadFormatIndicator> getPayloadFormatIndicator() {
            return Optional.empty();
        }

        @Override
        public Optional<Long> getMessageExpiryInterval() {
            return Optional.empty();
        }

        @Override
        public Optional<String> getResponseTopic() {
            return Optional.empty();
        }

        @Override
        public Optional<ByteBuffer> getCorrelationData() {
            return Optional.empty();
        }

        @Override
        public Optional<String> getContentType() {
            return Optional.empty();
        }

        @Override
        public Optional<ByteBuffer> getPayload() {
            return Optional.empty();
        }

        @Override
        public UserProperties getUserProperties() {
            return null;
        }
    }
}