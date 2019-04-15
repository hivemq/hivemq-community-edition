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

package com.hivemq.mqtt.handler.ordering;

import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.codec.encoder.mqtt5.PublishDroppedEvent;
import com.hivemq.mqtt.handler.publish.PublishStatus;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.message.publish.PublishWithFuture;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import util.InitFutureUtilsExecutorRule;
import util.TestMessageUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Dominik Obermaier
 */
public class OrderedTopicHandlerTest {

    @Rule
    public InitFutureUtilsExecutorRule initFutureUtilsExecutorRule = new InitFutureUtilsExecutorRule();

    private Channel channel;
    private OrderedTopicHandler orderedTopicHandler;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        final DefaultEventLoopGroup eventExecutors = new DefaultEventLoopGroup(1);

        channel = new EmbeddedChannel();

        channel.attr(ChannelAttributes.CLIENT_ID).getAndSet("client");
        eventExecutors.register(channel);

        orderedTopicHandler = new OrderedTopicHandler();
        channel.pipeline().addFirst(orderedTopicHandler);
    }

    @Test(timeout = 5000)
    public void test_qos1_release_next_message_on_next_puback() throws Exception {

        final PUBLISH publish = createPublish("topic", 1, QoS.AT_LEAST_ONCE);
        final PUBLISH publish2 = createPublish("topic", 2, QoS.AT_LEAST_ONCE);
        final PUBLISH publish3 = createPublish("topic", 3, QoS.AT_LEAST_ONCE);

        final ChannelPromise promise1 = channel.newPromise();
        final ChannelPromise promise2 = channel.newPromise();
        final ChannelPromise promise3 = channel.newPromise();

        channel.writeAndFlush(publish, promise1);
        channel.writeAndFlush(publish2, promise2);
        channel.writeAndFlush(publish3, promise3);

        channel.pipeline().fireChannelRead(new PUBACK(1));
        channel.pipeline().fireChannelRead(new PUBACK(2));

        promise1.await();
        promise2.await();
        promise3.await();
    }

    @Test(timeout = 5000)
    public void test_qos1_release_next_message_on_dropped() throws Exception {

        final PUBLISH publish = createPublish("topic", 1, QoS.AT_LEAST_ONCE);
        final PUBLISH publish2 = createPublish("topic", 2, QoS.AT_LEAST_ONCE);
        final PUBLISH publish3 = createPublish("topic", 3, QoS.AT_LEAST_ONCE);

        final ChannelPromise promise1 = channel.newPromise();
        final ChannelPromise promise2 = channel.newPromise();
        final ChannelPromise promise3 = channel.newPromise();

        channel.writeAndFlush(publish, promise1);
        channel.writeAndFlush(publish2, promise2);
        channel.writeAndFlush(publish3, promise3);

        channel.pipeline().fireUserEventTriggered(new PublishDroppedEvent(publish));
        channel.pipeline().fireUserEventTriggered(new PublishDroppedEvent(publish2));

        promise1.await();
        promise2.await();
        promise3.await();
    }

    @Test(timeout = 5000)
    public void test_qos1_release_use_different_buckets_no_queuing() throws Exception {

        final PUBLISH publish = createPublish("bucket_zero", 1, QoS.AT_LEAST_ONCE);
        final PUBLISH publish2 = createPublish("bucket=1", 2, QoS.AT_LEAST_ONCE);
        final PUBLISH publish3 = createPublish("bucket-2", 3, QoS.AT_LEAST_ONCE);

        final ChannelPromise promise1 = channel.newPromise();
        final ChannelPromise promise2 = channel.newPromise();
        final ChannelPromise promise3 = channel.newPromise();

        channel.writeAndFlush(publish, promise1);
        channel.writeAndFlush(publish2, promise2);
        channel.writeAndFlush(publish3, promise3);

        promise1.await();
        promise2.await();
        promise3.await();

    }

    @Test(timeout = 5000)
    public void test_qos1_send_puback_queued_messages() throws Exception {


        final PUBLISH publish = createPublish("bucket_zero", 1, QoS.AT_LEAST_ONCE);
        final PUBLISH publish2 = createPublish("bucket=1", 2, QoS.AT_LEAST_ONCE);
        final PUBLISH publish3 = createPublish("bucket-2", 3, QoS.AT_LEAST_ONCE);

        final ChannelPromise promise1 = channel.newPromise();
        final ChannelPromise promise2 = channel.newPromise();
        final ChannelPromise promise3 = channel.newPromise();

        channel.writeAndFlush(publish, promise1);
        channel.writeAndFlush(publish2, promise2);
        channel.writeAndFlush(publish3, promise3);

        channel.pipeline().fireChannelRead(new PUBACK(1));
        channel.pipeline().fireChannelRead(new PUBACK(1));
        channel.pipeline().fireChannelRead(new PUBACK(1));
        channel.pipeline().fireChannelRead(new PUBACK(2));

        promise1.await();
        promise2.await();
        promise3.await();

    }

    @Test(timeout = 5000)
    public void test_qos1_send_puback_queued_messages_multiple_pubacks() throws Exception {


        final PUBLISH publish = createPublish("bucket_zero", 1, QoS.AT_LEAST_ONCE);
        final PUBLISH publish2 = createPublish("bucket=1", 2, QoS.AT_LEAST_ONCE);
        final PUBLISH publish3 = createPublish("bucket-2", 3, QoS.AT_LEAST_ONCE);
        final PUBLISH publish4 = createPublish("bucket_zero", 4, QoS.AT_LEAST_ONCE);
        final PUBLISH publish5 = createPublish("bucket=1", 5, QoS.AT_LEAST_ONCE);
        final PUBLISH publish6 = createPublish("bucket-2", 6, QoS.AT_LEAST_ONCE);

        final ChannelPromise promise1 = channel.newPromise();
        final ChannelPromise promise2 = channel.newPromise();
        final ChannelPromise promise3 = channel.newPromise();
        final ChannelPromise promise4 = channel.newPromise();
        final ChannelPromise promise5 = channel.newPromise();
        final ChannelPromise promise6 = channel.newPromise();

        channel.writeAndFlush(publish, promise1);
        channel.writeAndFlush(publish2, promise2);
        channel.writeAndFlush(publish3, promise3);
        channel.writeAndFlush(publish4, promise4);
        channel.writeAndFlush(publish5, promise5);
        channel.writeAndFlush(publish6, promise6);

        channel.pipeline().fireChannelRead(new PUBACK(1));
        channel.pipeline().fireChannelRead(new PUBACK(1));
        channel.pipeline().fireChannelRead(new PUBACK(1));
        channel.pipeline().fireChannelRead(new PUBACK(2));

        channel.pipeline().fireChannelRead(new PUBACK(4));
        channel.pipeline().fireChannelRead(new PUBACK(4));
        channel.pipeline().fireChannelRead(new PUBACK(5));
        channel.pipeline().fireChannelRead(new PUBACK(5));
        channel.pipeline().fireChannelRead(new PUBACK(6));
        channel.pipeline().fireChannelRead(new PUBACK(6));

        promise1.await();
        promise2.await();
        promise3.await();
        promise4.await();
        promise5.await();
        promise6.await();

    }

    @Test(timeout = 5000)
    public void test_qos1_queue_different_topics_on_same_bucket() throws Exception {


        final PUBLISH publish = createPublish("bucket_zero", 1, QoS.AT_LEAST_ONCE);
        final PUBLISH publish2 = createPublish("bucket-0", 2, QoS.AT_LEAST_ONCE);
        final PUBLISH publish3 = createPublish("bucket%-zero", 3, QoS.AT_LEAST_ONCE);

        final ChannelPromise promise1 = channel.newPromise();
        final ChannelPromise promise2 = channel.newPromise();
        final ChannelPromise promise3 = channel.newPromise();

        channel.writeAndFlush(publish, promise1);
        channel.writeAndFlush(publish2, promise2);
        channel.writeAndFlush(publish3, promise3);


        channel.pipeline().fireChannelRead(new PUBACK(1));
        channel.pipeline().fireChannelRead(new PUBACK(2));

        promise1.await();
        promise2.await();
        promise3.await();

        assertEquals(true, orderedTopicHandler.getQueues()[0].isEmpty());
        assertEquals(null, orderedTopicHandler.getQueues()[1]);
        assertEquals(null, orderedTopicHandler.getQueues()[2]);

    }

    @Test(timeout = 4000)
    public void test_remove_messages() throws Exception {
        final PUBLISH publish1 = createPublish("bucket_zero", 1, QoS.AT_LEAST_ONCE);
        final PUBLISH publish2 = createPublish("bucket_zero", 2, QoS.AT_LEAST_ONCE);
        final PUBLISH publish3 = createPublish("bucket_zero", 3, QoS.AT_LEAST_ONCE);
        final PUBLISH publish4 = createPublish("bucket_zero", 4, QoS.AT_LEAST_ONCE);

        final ChannelPromise promise1 = channel.newPromise();
        final ChannelPromise promise2 = channel.newPromise();
        final ChannelPromise promise3 = channel.newPromise();
        final ChannelPromise promise4 = channel.newPromise();


        channel.writeAndFlush(publish1, promise1);
        channel.writeAndFlush(publish2, promise2);
        channel.writeAndFlush(publish3, promise3);
        channel.writeAndFlush(publish4, promise4);


        promise1.await();

        assertEquals(3, orderedTopicHandler.getQueues()[0].size());
        channel.pipeline().fireChannelRead(new PUBACK(1));

        promise2.await();
        assertEquals(2, orderedTopicHandler.getQueues()[0].size());

        channel.pipeline().fireChannelRead(new PUBACK(2));
        promise3.await();

        channel.pipeline().fireChannelRead(new PUBACK(4));
        promise4.await();


        assertTrue(orderedTopicHandler.getQueues()[0].isEmpty());
    }

    @Test(timeout = 5000)
    public void test_qos2_release_next_message_on_next_pubcomp() throws Exception {

        final PUBLISH publish = createPublish("topic", 1, QoS.EXACTLY_ONCE);
        final PUBLISH publish2 = createPublish("topic", 2, QoS.EXACTLY_ONCE);
        final PUBLISH publish3 = createPublish("topic", 3, QoS.EXACTLY_ONCE);

        final ChannelPromise promise1 = channel.newPromise();
        final ChannelPromise promise2 = channel.newPromise();
        final ChannelPromise promise3 = channel.newPromise();

        channel.writeAndFlush(publish, promise1);
        channel.writeAndFlush(publish2, promise2);
        channel.writeAndFlush(publish3, promise3);

        channel.pipeline().fireChannelRead(new PUBCOMP(1));
        channel.pipeline().fireChannelRead(new PUBCOMP(2));

        promise1.await();
        promise2.await();
        promise3.await();
    }

    @Test(timeout = 5000)
    public void test_do_not_queue_resended_messages() throws Exception {

        final PUBLISH publish = createPublish("bucket_zero", 1, QoS.EXACTLY_ONCE);

        final ChannelPromise promise1 = channel.newPromise();
        final ChannelPromise promise2 = channel.newPromise();

        channel.writeAndFlush(publish, promise1);


        //resend message with same message id
        final PUBLISH publish2 = createPublish("bucket_zero", 1, QoS.EXACTLY_ONCE, true);
        channel.writeAndFlush(publish2, promise2);

        promise1.await();
        promise2.await();
    }

    @Test(timeout = 5000)
    public void test_qos1_return_publish_status_on_puback() throws Exception {

        final PUBLISH publish = createPublish("topic", 1, QoS.AT_LEAST_ONCE);
        final PUBLISH internalPublish = TestMessageUtil.createMqtt3Publish("hivemqId", publish);
        final SettableFuture<PublishStatus> future = SettableFuture.create();
        final PublishWithFuture publishWithFuture = new PublishWithFuture(internalPublish, future, true);

        final ChannelPromise promise1 = channel.newPromise();

        channel.writeAndFlush(publishWithFuture, promise1);

        channel.pipeline().fireChannelRead(new PUBACK(1));

        promise1.await();

        assertEquals(PublishStatus.DELIVERED, future.get());
    }

    @Test(timeout = 5000)
    public void test_qos2_return_publish_status_on_pubcomp() throws Exception {

        final PUBLISH publish = createPublish("topic", 1, QoS.EXACTLY_ONCE);
        final PUBLISH internalPublish = TestMessageUtil.createMqtt3Publish("hivemqId", publish);
        final SettableFuture<PublishStatus> future = SettableFuture.create();
        final PublishWithFuture publishWithFuture = new PublishWithFuture(internalPublish, future, true);

        final ChannelPromise promise1 = channel.newPromise();

        channel.writeAndFlush(publishWithFuture, promise1);

        channel.pipeline().fireChannelRead(new PUBCOMP(1));

        promise1.await();

        assertEquals(PublishStatus.DELIVERED, future.get());
    }

    private PUBLISH createPublish(final String topic, final int messageId, final QoS qoS) {
        return createPublish(topic, messageId, qoS, false);
    }

    private PUBLISH createPublish(final String topic, final int messageId, final QoS qoS, final boolean dup) {

        return new PUBLISHFactory.Mqtt3Builder()
                .withHivemqId("hivemqId")
                .withMessageExpiryInterval(PUBLISH.MESSAGE_EXPIRY_INTERVAL_MAX)
                .withTopic(topic)
                .withQoS(qoS)
                .withPacketIdentifier(messageId)
                .withPayload("payload".getBytes())
                .withDuplicateDelivery(dup)
                .build();
    }

}