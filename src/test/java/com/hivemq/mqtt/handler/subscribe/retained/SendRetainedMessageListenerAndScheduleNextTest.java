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
package com.hivemq.mqtt.handler.subscribe.retained;

import com.google.common.util.concurrent.Futures;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.pool.exception.NoMessageIdAvailableException;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.Channel;
import io.netty.channel.DefaultEventLoop;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.TestChannelAttribute;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executors;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Lukas Brandl
 */
public class SendRetainedMessageListenerAndScheduleNextTest {

    @Mock
    private RetainedMessagesSender retainedMessagesSender;

    @Mock
    private Channel channel;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(channel.eventLoop()).thenReturn(new DefaultEventLoop(Executors.newSingleThreadExecutor()));
    }

    @Test
    public void success() {
        when(channel.isActive()).thenReturn(true);
        when(retainedMessagesSender.writeRetainedMessages(any(Channel.class), Matchers.<Topic>anyVararg())).thenReturn(
                Futures.immediateFuture(null));
        final Topic topic = new Topic("#", QoS.AT_LEAST_ONCE);
        final Queue<String> topics = new ArrayDeque<>();
        for (int i = 0; i < 90; i++) {
            topics.add("topic" + i);
        }
        final SendRetainedMessageListenerAndScheduleNext listener =
                new SendRetainedMessageListenerAndScheduleNext(topic, topics, channel, retainedMessagesSender, 25);
        listener.onSuccess(null);

        verify(retainedMessagesSender, timeout(5000).times(4)).writeRetainedMessages(
                eq(channel), Matchers.<Topic>anyVararg());
    }

    @Test
    public void success_channel_inactive() {
        when(channel.isActive()).thenReturn(false);
        final Topic topic = new Topic("#", QoS.AT_LEAST_ONCE);
        final Queue<String> topics = new ArrayDeque<>();
        for (int i = 0; i < 90; i++) {
            topics.add("topic" + i);
        }
        final SendRetainedMessageListenerAndScheduleNext listener =
                new SendRetainedMessageListenerAndScheduleNext(topic, topics, channel, retainedMessagesSender, 25);
        listener.onSuccess(null);

        verify(retainedMessagesSender, never()).writeRetainedMessages(
                any(Channel.class), Matchers.<Topic>anyVararg());
    }

    @Test
    public void failure() {
        when(channel.isActive()).thenReturn(true);
        when(channel.attr(ChannelAttributes.CLIENT_ID)).thenReturn(new TestChannelAttribute<>("client"));
        final Topic topic = new Topic("#", QoS.AT_LEAST_ONCE);
        final Queue<String> topics = new ArrayDeque<>();
        for (int i = 0; i < 90; i++) {
            topics.add("topic" + i);
        }
        final SendRetainedMessageListenerAndScheduleNext listener =
                new SendRetainedMessageListenerAndScheduleNext(topic, topics, channel, retainedMessagesSender, 25);
        listener.onFailure(new RuntimeException("test"));

        verify(retainedMessagesSender, never()).writeRetainedMessages(
                any(Channel.class), Matchers.<Topic>anyVararg());
        verify(channel).disconnect();
    }

    @Test
    public void failure_no_more_message_id() {
        when(channel.isActive()).thenReturn(true);
        when(channel.attr(ChannelAttributes.CLIENT_ID)).thenReturn(new TestChannelAttribute<>("client"));
        when(retainedMessagesSender.writeRetainedMessages(any(Channel.class), Matchers.<Topic>anyVararg())).thenReturn(
                Futures.immediateFuture(null));
        final Topic topic = new Topic("#", QoS.AT_LEAST_ONCE);
        final Queue<String> topics = new ArrayDeque<>();
        for (int i = 0; i < 90; i++) {
            topics.add("topic" + i);
        }
        final SendRetainedMessageListenerAndScheduleNext listener =
                new SendRetainedMessageListenerAndScheduleNext(topic, topics, channel, retainedMessagesSender, 25);
        listener.onFailure(new NoMessageIdAvailableException());

        verify(retainedMessagesSender, timeout(5000).times(4)).writeRetainedMessages(
                any(Channel.class), Matchers.<Topic>anyVararg());
    }
}