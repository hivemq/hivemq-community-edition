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

package com.hivemq.extensions.interceptor.publish.parameter;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.mqtt.message.publish.PUBLISH;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.TestConfigurationBootstrap;
import util.TestMessageUtil;

import java.time.Duration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class PublishInboundOutputImplTest {

    private PublishInboundOutputImpl publishInboundOutput;

    private FullConfigurationService config;

    @Mock
    private PluginOutPutAsyncer pluginOutPutAsyncer;

    private PUBLISH origin;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        config = new TestConfigurationBootstrap().getFullConfigurationService();
        origin = TestMessageUtil.createFullMqtt5Publish();
        publishInboundOutput = new PublishInboundOutputImpl(config, pluginOutPutAsyncer, origin);
        assertEquals(publishInboundOutput, publishInboundOutput.get());
    }

    @Test
    public void test_async_duration() {

        publishInboundOutput.async(Duration.ofSeconds(10));

        assertEquals(TimeoutFallback.FAILURE, publishInboundOutput.getTimeoutFallback());
        assertEquals(AckReasonCode.SUCCESS, publishInboundOutput.getReasonCode());

    }

    @Test
    public void test_async_duration_fallback_failure() {

        publishInboundOutput.async(Duration.ofSeconds(10), TimeoutFallback.FAILURE);

        assertEquals(TimeoutFallback.FAILURE, publishInboundOutput.getTimeoutFallback());
        assertEquals(AckReasonCode.SUCCESS, publishInboundOutput.getReasonCode());

    }

    @Test
    public void test_async_duration_fallback_success() {

        publishInboundOutput.async(Duration.ofSeconds(10), TimeoutFallback.SUCCESS);

        assertEquals(TimeoutFallback.SUCCESS, publishInboundOutput.getTimeoutFallback());
        assertEquals(AckReasonCode.SUCCESS, publishInboundOutput.getReasonCode());

    }

    @Test(expected = IllegalArgumentException.class)
    public void test_async_duration_fallback_success_reason_string_failed() {

        publishInboundOutput.async(Duration.ofSeconds(10), TimeoutFallback.SUCCESS, AckReasonCode.SUCCESS, "reason");

    }

    @Test(expected = IllegalArgumentException.class)
    public void test_async_duration_fallback_failure_reason_string_failed() {

        publishInboundOutput.async(Duration.ofSeconds(10), TimeoutFallback.FAILURE, AckReasonCode.SUCCESS, "reason");

    }

    @Test(expected = IllegalArgumentException.class)
    public void test_async_duration_fallback_success_ack_failed() {

        publishInboundOutput.async(Duration.ofSeconds(10), TimeoutFallback.SUCCESS, AckReasonCode.NOT_AUTHORIZED, "reason");

    }

    @Test
    public void test_async_duration_fallback_failed_ack_failed_with_reason() {

        publishInboundOutput.async(Duration.ofSeconds(10), TimeoutFallback.FAILURE, AckReasonCode.NOT_AUTHORIZED, "reason");

        assertEquals(TimeoutFallback.FAILURE, publishInboundOutput.getTimeoutFallback());
        assertEquals(AckReasonCode.NOT_AUTHORIZED, publishInboundOutput.getReasonCode());
        assertEquals("reason", publishInboundOutput.getReasonString());

    }

    @Test
    public void test_async_duration_fallback_failed_ack_failed_without_reason() {

        publishInboundOutput.async(Duration.ofSeconds(10), TimeoutFallback.FAILURE, AckReasonCode.NOT_AUTHORIZED);

        assertEquals(TimeoutFallback.FAILURE, publishInboundOutput.getTimeoutFallback());
        assertEquals(AckReasonCode.NOT_AUTHORIZED, publishInboundOutput.getReasonCode());
        assertEquals(null, publishInboundOutput.getReasonString());

    }

    @Test
    public void test_prevent_delivery_default() {

        publishInboundOutput.preventPublishDelivery();

        assertEquals(AckReasonCode.SUCCESS, publishInboundOutput.getReasonCode());
        assertEquals(null, publishInboundOutput.getReasonString());
        assertTrue(publishInboundOutput.isPreventDelivery());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_prevent_delivery_twice() {

        publishInboundOutput.preventPublishDelivery();
        publishInboundOutput.preventPublishDelivery();

    }

    @Test
    public void test_forcibly_prevention_after_default() {

        publishInboundOutput.preventPublishDelivery();
        publishInboundOutput.forciblyPreventPublishDelivery(AckReasonCode.QUOTA_EXCEEDED, "reason");

        assertEquals(AckReasonCode.QUOTA_EXCEEDED, publishInboundOutput.getReasonCode());
        assertEquals("reason", publishInboundOutput.getReasonString());

    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_async_twice() {

        publishInboundOutput.async(Duration.ZERO);
        publishInboundOutput.async(Duration.ofMinutes(1), TimeoutFallback.SUCCESS);

    }

    @Test(expected = IllegalArgumentException.class)
    public void test_prevent_delivery_success_with_reason_string() {

        publishInboundOutput.preventPublishDelivery(AckReasonCode.SUCCESS, "reason");

    }

    @Test
    public void test_prevent_delivery_with_reason_code() {

        publishInboundOutput.preventPublishDelivery(AckReasonCode.IMPLEMENTATION_SPECIFIC_ERROR);

        assertEquals(AckReasonCode.IMPLEMENTATION_SPECIFIC_ERROR, publishInboundOutput.getReasonCode());
        assertEquals(null, publishInboundOutput.getReasonString());
        assertTrue(publishInboundOutput.isPreventDelivery());
    }

    @Test
    public void test_prevent_delivery_with_reason_code_and_reason_string() {

        publishInboundOutput.preventPublishDelivery(AckReasonCode.IMPLEMENTATION_SPECIFIC_ERROR, "reason");

        assertEquals(AckReasonCode.IMPLEMENTATION_SPECIFIC_ERROR, publishInboundOutput.getReasonCode());
        assertEquals("reason", publishInboundOutput.getReasonString());
        assertTrue(publishInboundOutput.isPreventDelivery());
    }

    @Test(expected = NullPointerException.class)
    public void test_prevent_delivery_with_reason_code_null() {
        publishInboundOutput.preventPublishDelivery(null);
    }

    @Test(expected = NullPointerException.class)
    public void test_prevent_delivery_with_reason_code_null_and_reason_string() {
        publishInboundOutput.preventPublishDelivery(null, "reason");
    }
}