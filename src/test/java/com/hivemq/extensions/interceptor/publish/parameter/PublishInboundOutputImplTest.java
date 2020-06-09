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
package com.hivemq.extensions.interceptor.publish.parameter;

import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.packets.publish.ModifiablePublishPacketImpl;
import com.hivemq.extensions.packets.publish.PublishPacketImpl;
import org.junit.Test;

import java.time.Duration;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Florian Limpöck
 * @author Silvio Giebl
 */
public class PublishInboundOutputImplTest {

    @Test
    public void constructor_and_getter() {
        final PluginOutPutAsyncer asyncer = mock(PluginOutPutAsyncer.class);
        final ModifiablePublishPacketImpl modifiablePacket = mock(ModifiablePublishPacketImpl.class);

        final PublishInboundOutputImpl output = new PublishInboundOutputImpl(asyncer, modifiablePacket);

        assertSame(modifiablePacket, output.getPublishPacket());
    }

    @Test
    public void update() {
        final PluginOutPutAsyncer asyncer = mock(PluginOutPutAsyncer.class);
        final ModifiablePublishPacketImpl modifiablePacket = mock(ModifiablePublishPacketImpl.class);

        final PublishInboundOutputImpl output = new PublishInboundOutputImpl(asyncer, modifiablePacket);

        final PublishInboundInputImpl input = mock(PublishInboundInputImpl.class);
        final PublishPacketImpl packet = mock(PublishPacketImpl.class);
        final ModifiablePublishPacketImpl newModifiablePacket = mock(ModifiablePublishPacketImpl.class);
        when(input.getPublishPacket()).thenReturn(packet);
        when(modifiablePacket.update(packet)).thenReturn(newModifiablePacket);

        final PublishInboundOutputImpl updated = output.update(input);

        assertSame(newModifiablePacket, updated.getPublishPacket());
    }

    @Test
    public void test_async_duration() {
        final PluginOutPutAsyncer asyncer = mock(PluginOutPutAsyncer.class);
        final ModifiablePublishPacketImpl modifiablePacket = mock(ModifiablePublishPacketImpl.class);

        final PublishInboundOutputImpl output = new PublishInboundOutputImpl(asyncer, modifiablePacket);

        output.async(Duration.ofSeconds(10));

        assertEquals(TimeoutFallback.FAILURE, output.getTimeoutFallback());
        assertEquals(AckReasonCode.SUCCESS, output.getReasonCode());
    }

    @Test
    public void test_async_duration_fallback_failure() {
        final PluginOutPutAsyncer asyncer = mock(PluginOutPutAsyncer.class);
        final ModifiablePublishPacketImpl modifiablePacket = mock(ModifiablePublishPacketImpl.class);

        final PublishInboundOutputImpl output = new PublishInboundOutputImpl(asyncer, modifiablePacket);

        output.async(Duration.ofSeconds(10), TimeoutFallback.FAILURE);

        assertEquals(TimeoutFallback.FAILURE, output.getTimeoutFallback());
        assertEquals(AckReasonCode.SUCCESS, output.getReasonCode());
    }

    @Test
    public void test_async_duration_fallback_success() {
        final PluginOutPutAsyncer asyncer = mock(PluginOutPutAsyncer.class);
        final ModifiablePublishPacketImpl modifiablePacket = mock(ModifiablePublishPacketImpl.class);

        final PublishInboundOutputImpl output = new PublishInboundOutputImpl(asyncer, modifiablePacket);

        output.async(Duration.ofSeconds(10), TimeoutFallback.SUCCESS);

        assertEquals(TimeoutFallback.SUCCESS, output.getTimeoutFallback());
        assertEquals(AckReasonCode.SUCCESS, output.getReasonCode());
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_async_duration_fallback_success_reason_string_failed() {
        final PluginOutPutAsyncer asyncer = mock(PluginOutPutAsyncer.class);
        final ModifiablePublishPacketImpl modifiablePacket = mock(ModifiablePublishPacketImpl.class);

        final PublishInboundOutputImpl output = new PublishInboundOutputImpl(asyncer, modifiablePacket);

        output.async(Duration.ofSeconds(10), TimeoutFallback.SUCCESS, AckReasonCode.SUCCESS, "reason");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_async_duration_fallback_failure_reason_string_failed() {
        final PluginOutPutAsyncer asyncer = mock(PluginOutPutAsyncer.class);
        final ModifiablePublishPacketImpl modifiablePacket = mock(ModifiablePublishPacketImpl.class);

        final PublishInboundOutputImpl output = new PublishInboundOutputImpl(asyncer, modifiablePacket);

        output.async(Duration.ofSeconds(10), TimeoutFallback.FAILURE, AckReasonCode.SUCCESS, "reason");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_async_duration_fallback_success_ack_failed() {
        final PluginOutPutAsyncer asyncer = mock(PluginOutPutAsyncer.class);
        final ModifiablePublishPacketImpl modifiablePacket = mock(ModifiablePublishPacketImpl.class);

        final PublishInboundOutputImpl output = new PublishInboundOutputImpl(asyncer, modifiablePacket);

        output.async(Duration.ofSeconds(10), TimeoutFallback.SUCCESS, AckReasonCode.NOT_AUTHORIZED, "reason");
    }

    @Test
    public void test_async_duration_fallback_failed_ack_failed_with_reason() {
        final PluginOutPutAsyncer asyncer = mock(PluginOutPutAsyncer.class);
        final ModifiablePublishPacketImpl modifiablePacket = mock(ModifiablePublishPacketImpl.class);

        final PublishInboundOutputImpl output = new PublishInboundOutputImpl(asyncer, modifiablePacket);

        output.async(Duration.ofSeconds(10), TimeoutFallback.FAILURE, AckReasonCode.NOT_AUTHORIZED, "reason");

        assertEquals(TimeoutFallback.FAILURE, output.getTimeoutFallback());
        assertEquals(AckReasonCode.NOT_AUTHORIZED, output.getReasonCode());
        assertEquals("reason", output.getReasonString());
    }

    @Test
    public void test_async_duration_fallback_failed_ack_failed_without_reason() {
        final PluginOutPutAsyncer asyncer = mock(PluginOutPutAsyncer.class);
        final ModifiablePublishPacketImpl modifiablePacket = mock(ModifiablePublishPacketImpl.class);

        final PublishInboundOutputImpl output = new PublishInboundOutputImpl(asyncer, modifiablePacket);

        output.async(Duration.ofSeconds(10), TimeoutFallback.FAILURE, AckReasonCode.NOT_AUTHORIZED);

        assertEquals(TimeoutFallback.FAILURE, output.getTimeoutFallback());
        assertEquals(AckReasonCode.NOT_AUTHORIZED, output.getReasonCode());
        assertEquals(null, output.getReasonString());
    }

    @Test
    public void test_prevent_delivery_default() {
        final PluginOutPutAsyncer asyncer = mock(PluginOutPutAsyncer.class);
        final ModifiablePublishPacketImpl modifiablePacket = mock(ModifiablePublishPacketImpl.class);

        final PublishInboundOutputImpl output = new PublishInboundOutputImpl(asyncer, modifiablePacket);

        output.preventPublishDelivery();

        assertEquals(AckReasonCode.SUCCESS, output.getReasonCode());
        assertEquals(null, output.getReasonString());
        assertTrue(output.isPreventDelivery());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_prevent_delivery_twice() {
        final PluginOutPutAsyncer asyncer = mock(PluginOutPutAsyncer.class);
        final ModifiablePublishPacketImpl modifiablePacket = mock(ModifiablePublishPacketImpl.class);

        final PublishInboundOutputImpl output = new PublishInboundOutputImpl(asyncer, modifiablePacket);

        output.preventPublishDelivery();
        output.preventPublishDelivery();
    }

    @Test
    public void test_forcibly_prevention_after_default() {
        final PluginOutPutAsyncer asyncer = mock(PluginOutPutAsyncer.class);
        final ModifiablePublishPacketImpl modifiablePacket = mock(ModifiablePublishPacketImpl.class);

        final PublishInboundOutputImpl output = new PublishInboundOutputImpl(asyncer, modifiablePacket);

        output.preventPublishDelivery();
        output.forciblyPreventPublishDelivery(AckReasonCode.QUOTA_EXCEEDED, "reason");

        assertEquals(AckReasonCode.QUOTA_EXCEEDED, output.getReasonCode());
        assertEquals("reason", output.getReasonString());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_async_twice() {
        final PluginOutPutAsyncer asyncer = mock(PluginOutPutAsyncer.class);
        final ModifiablePublishPacketImpl modifiablePacket = mock(ModifiablePublishPacketImpl.class);

        final PublishInboundOutputImpl output = new PublishInboundOutputImpl(asyncer, modifiablePacket);

        output.async(Duration.ZERO);
        output.async(Duration.ofMinutes(1), TimeoutFallback.SUCCESS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_prevent_delivery_success_with_reason_string() {
        final PluginOutPutAsyncer asyncer = mock(PluginOutPutAsyncer.class);
        final ModifiablePublishPacketImpl modifiablePacket = mock(ModifiablePublishPacketImpl.class);

        final PublishInboundOutputImpl output = new PublishInboundOutputImpl(asyncer, modifiablePacket);

        output.preventPublishDelivery(AckReasonCode.SUCCESS, "reason");
    }

    @Test
    public void test_prevent_delivery_with_reason_code() {
        final PluginOutPutAsyncer asyncer = mock(PluginOutPutAsyncer.class);
        final ModifiablePublishPacketImpl modifiablePacket = mock(ModifiablePublishPacketImpl.class);

        final PublishInboundOutputImpl output = new PublishInboundOutputImpl(asyncer, modifiablePacket);

        output.preventPublishDelivery(AckReasonCode.IMPLEMENTATION_SPECIFIC_ERROR);

        assertEquals(AckReasonCode.IMPLEMENTATION_SPECIFIC_ERROR, output.getReasonCode());
        assertEquals(null, output.getReasonString());
        assertTrue(output.isPreventDelivery());
    }

    @Test
    public void test_prevent_delivery_with_reason_code_and_reason_string() {
        final PluginOutPutAsyncer asyncer = mock(PluginOutPutAsyncer.class);
        final ModifiablePublishPacketImpl modifiablePacket = mock(ModifiablePublishPacketImpl.class);

        final PublishInboundOutputImpl output = new PublishInboundOutputImpl(asyncer, modifiablePacket);

        output.preventPublishDelivery(AckReasonCode.IMPLEMENTATION_SPECIFIC_ERROR, "reason");

        assertEquals(AckReasonCode.IMPLEMENTATION_SPECIFIC_ERROR, output.getReasonCode());
        assertEquals("reason", output.getReasonString());
        assertTrue(output.isPreventDelivery());
    }

    @Test(expected = NullPointerException.class)
    public void test_prevent_delivery_with_reason_code_null() {
        final PluginOutPutAsyncer asyncer = mock(PluginOutPutAsyncer.class);
        final ModifiablePublishPacketImpl modifiablePacket = mock(ModifiablePublishPacketImpl.class);

        final PublishInboundOutputImpl output = new PublishInboundOutputImpl(asyncer, modifiablePacket);

        output.preventPublishDelivery(null);
    }

    @Test(expected = NullPointerException.class)
    public void test_prevent_delivery_with_reason_code_null_and_reason_string() {
        final PluginOutPutAsyncer asyncer = mock(PluginOutPutAsyncer.class);
        final ModifiablePublishPacketImpl modifiablePacket = mock(ModifiablePublishPacketImpl.class);

        final PublishInboundOutputImpl output = new PublishInboundOutputImpl(asyncer, modifiablePacket);

        output.preventPublishDelivery(null, "reason");
    }
}