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

package com.hivemq.extensions.handler.tasks;

import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectReasonCode;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
import com.hivemq.extensions.auth.parameter.PublishAuthorizerOutputImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginOutputAsyncerImpl;
import com.hivemq.extensions.handler.PluginAuthorizerServiceImpl.AuthorizeWillResultEvent;
import com.hivemq.mqtt.message.connect.CONNECT;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import util.CollectUserEventsHandler;
import util.TestMessageUtil;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class WillPublishAuthorizationProcessedTaskTest {

    private @NotNull CollectUserEventsHandler<AuthorizeWillResultEvent> eventsCollector;
    private @NotNull EmbeddedChannel channel;
    private @NotNull WillPublishAuthorizationProcessedTask task;
    private @NotNull PublishAuthorizerOutputImpl output;

    @Before
    public void before() {
        eventsCollector = new CollectUserEventsHandler<>(AuthorizeWillResultEvent.class);
        channel = new EmbeddedChannel(eventsCollector);
        final ChannelHandlerContext ctx = channel.pipeline().context(CollectUserEventsHandler.class);
        final CONNECT connect = TestMessageUtil.createMqtt5ConnectWithWill();
        task = new WillPublishAuthorizationProcessedTask(connect, ctx);

        final PluginOutPutAsyncer asyncer = new PluginOutputAsyncerImpl(mock(ShutdownHooks.class));
        output = new PublishAuthorizerOutputImpl(asyncer);
    }

    @Test
    public void test_disconnect() {
        output.authorizerPresent();
        output.disconnectClient();
        task.onSuccess(output);

        final AuthorizeWillResultEvent resultEvent = eventsCollector.pollEvent();
        assertNotNull(resultEvent);
        assertEquals(AckReasonCode.NOT_AUTHORIZED, resultEvent.getResult().getAckReasonCode());
        assertTrue(resultEvent.getResult().isAuthorizerPresent());
    }

    @Test
    public void test_disconnect_code() {
        output.authorizerPresent();
        output.disconnectClient(DisconnectReasonCode.QUOTA_EXCEEDED);
        task.onSuccess(output);

        final AuthorizeWillResultEvent resultEvent = eventsCollector.pollEvent();
        assertNotNull(resultEvent);
        assertEquals(DisconnectReasonCode.QUOTA_EXCEEDED, resultEvent.getResult().getDisconnectReasonCode());
    }

    @Test
    public void test_disconnect_code_string() {
        output.authorizerPresent();
        output.disconnectClient(DisconnectReasonCode.QUOTA_EXCEEDED, "test-string");
        task.onSuccess(output);

        final AuthorizeWillResultEvent resultEvent = eventsCollector.pollEvent();
        assertNotNull(resultEvent);
        assertEquals(DisconnectReasonCode.QUOTA_EXCEEDED, resultEvent.getResult().getDisconnectReasonCode());
        assertEquals("test-string", resultEvent.getResult().getReasonString());
    }

    @Test
    public void test_fail() {
        output.authorizerPresent();
        output.failAuthorization();
        task.onSuccess(output);

        channel.runPendingTasks();

        final AuthorizeWillResultEvent resultEvent = eventsCollector.pollEvent();
        assertNotNull(resultEvent);
        assertEquals(AckReasonCode.NOT_AUTHORIZED, resultEvent.getResult().getAckReasonCode());
        assertEquals(
                "Not allowed to connect with Will Publish for unauthorized topic 'topic' with QoS '2' and retain 'false'",
                resultEvent.getResult().getReasonString());
    }

    @Test
    public void test_fail_code() {
        output.authorizerPresent();
        output.failAuthorization(AckReasonCode.TOPIC_NAME_INVALID);
        task.onSuccess(output);

        channel.runPendingTasks();

        final AuthorizeWillResultEvent resultEvent = eventsCollector.pollEvent();
        assertNotNull(resultEvent);
        assertEquals(AckReasonCode.TOPIC_NAME_INVALID, resultEvent.getResult().getAckReasonCode());
        assertEquals(
                "Not allowed to connect with Will Publish for unauthorized topic 'topic' with QoS '2' and retain 'false'",
                resultEvent.getResult().getReasonString());
    }

    @Test
    public void test_fail_code_string() {
        output.authorizerPresent();
        output.failAuthorization(AckReasonCode.TOPIC_NAME_INVALID, "test-string");
        task.onSuccess(output);

        channel.runPendingTasks();

        final AuthorizeWillResultEvent resultEvent = eventsCollector.pollEvent();
        assertNotNull(resultEvent);
        assertEquals(AckReasonCode.TOPIC_NAME_INVALID, resultEvent.getResult().getAckReasonCode());
        assertEquals("test-string", resultEvent.getResult().getReasonString());
    }

    @Test
    public void test_success() {
        output.authorizerPresent();
        output.authorizeSuccessfully();
        task.onSuccess(output);

        channel.runPendingTasks();

        final AuthorizeWillResultEvent resultEvent = eventsCollector.pollEvent();
        assertNotNull(resultEvent);
        assertEquals(AckReasonCode.SUCCESS, resultEvent.getResult().getAckReasonCode());
    }

    @Test
    public void test_next() {
        output.authorizerPresent();
        output.nextExtensionOrDefault();
        task.onSuccess(output);

        channel.runPendingTasks();

        final AuthorizeWillResultEvent resultEvent = eventsCollector.pollEvent();
        assertNotNull(resultEvent);
        assertTrue(resultEvent.getResult().isAuthorizerPresent());
        assertNull(resultEvent.getResult().getAckReasonCode());
        assertNull(resultEvent.getResult().getReasonString());
    }

    @Test
    public void test_undecided() {
        task.onSuccess(output);

        channel.runPendingTasks();

        final AuthorizeWillResultEvent resultEvent = eventsCollector.pollEvent();
        assertNotNull(resultEvent);
        assertFalse(resultEvent.getResult().isAuthorizerPresent());
        assertNull(resultEvent.getResult().getAckReasonCode());
        assertNull(resultEvent.getResult().getReasonString());
    }

    @Test
    public void test_undecided_authorizers_present() {
        output.authorizerPresent();
        task.onSuccess(output);

        channel.runPendingTasks();

        final AuthorizeWillResultEvent resultEvent = eventsCollector.pollEvent();
        assertNotNull(resultEvent);
        assertTrue(resultEvent.getResult().isAuthorizerPresent());
        assertEquals(AckReasonCode.NOT_AUTHORIZED, resultEvent.getResult().getAckReasonCode());
    }

    @Test
    public void test_failure() {
        task.onFailure(new RuntimeException("test"));

        channel.runPendingTasks();

        final AuthorizeWillResultEvent resultEvent = eventsCollector.pollEvent();
        assertNotNull(resultEvent);
        assertTrue(resultEvent.getResult().isAuthorizerPresent());
        assertEquals(AckReasonCode.NOT_AUTHORIZED, resultEvent.getResult().getAckReasonCode());
    }
}
