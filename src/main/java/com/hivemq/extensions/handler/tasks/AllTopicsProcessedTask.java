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

package com.hivemq.extensions.handler.tasks;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.bootstrap.netty.ChannelHandlerNames;
import com.hivemq.mqtt.handler.disconnect.Mqtt3ServerDisconnector;
import com.hivemq.mqtt.handler.disconnect.Mqtt5ServerDisconnector;
import com.hivemq.mqtt.handler.subscribe.SubscribeHandler;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5SubAckReasonCode;
import com.hivemq.mqtt.message.subscribe.SUBSCRIBE;
import com.hivemq.extensions.auth.parameter.SubscriptionAuthorizerOutputImpl;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Christoph Schäbel
 */
public class AllTopicsProcessedTask implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(AllTopicsProcessedTask.class);

    private final @NotNull SUBSCRIBE msg;
    private final @NotNull List<ListenableFuture<SubscriptionAuthorizerOutputImpl>> listenableFutures;
    private final @NotNull ChannelHandlerContext ctx;
    private final @NotNull Mqtt5ServerDisconnector mqtt5ServerDisconnector;
    private final @NotNull Mqtt3ServerDisconnector mqtt3ServerDisconnector;

    public AllTopicsProcessedTask(
            final @NotNull SUBSCRIBE msg,
            final @NotNull List<ListenableFuture<SubscriptionAuthorizerOutputImpl>> listenableFutures,
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull Mqtt5ServerDisconnector mqtt5ServerDisconnector,
            final @NotNull Mqtt3ServerDisconnector mqtt3ServerDisconnector) {

        this.msg = msg;
        this.listenableFutures = listenableFutures;
        this.ctx = ctx;
        this.mqtt5ServerDisconnector = mqtt5ServerDisconnector;
        this.mqtt3ServerDisconnector = mqtt3ServerDisconnector;
    }

    @Override
    public void run() {

        try {
            Preconditions.checkArgument(listenableFutures.size() == msg.getTopics().size(), "The amount of futures must be equal to the amount of topics");

            final Mqtt5SubAckReasonCode[] answerCodes = new Mqtt5SubAckReasonCode[msg.getTopics().size()];
            final String[] reasonStrings = new String[msg.getTopics().size()];

            boolean authorizersPresent = false;
            for (int i = 0; i < listenableFutures.size(); i++) {
                final SubscriptionAuthorizerOutputImpl output = listenableFutures.get(i).get();

                if (output.isAuthorizerPresent()) {
                    authorizersPresent = true;
                }

                switch (output.getAuthorizationState()) {
                    case CONTINUE:
                        break;
                    case DISCONNECT:
                        disconnectClient(i, output);
                        return;
                    case FAIL:
                        if (output.getSubackReasonCode() != null) {
                            answerCodes[i] = Mqtt5SubAckReasonCode.from(output.getSubackReasonCode());
                            reasonStrings[i] = output.getReasonString();
                        } else {
                            answerCodes[i] = Mqtt5SubAckReasonCode.NOT_AUTHORIZED;
                        }
                        break;
                    case UNDECIDED:
                        if (!output.isAuthorizerPresent()) {
                            //providers never returned an authorizer, same as continue
                            break;
                        }
                        answerCodes[i] = Mqtt5SubAckReasonCode.NOT_AUTHORIZED;
                        reasonStrings[i] = "Sent a SUBSCRIBE with an unauthorized subscription";
                        break;
                    case SUCCESS:
                        answerCodes[i] = Mqtt5SubAckReasonCode.fromCode(msg.getTopics().get(i).getQoS().getQosNumber());
                        break;
                    default:
                        break;
                }
            }

            final SubscribeHandler handler = (SubscribeHandler) ctx.pipeline().get(ChannelHandlerNames.MQTT_SUBSCRIBE_HANDLER);
            final boolean finalAuthorizersPresent = authorizersPresent;
            if (handler != null && ctx.channel().isActive()) {
                ctx.executor().execute(() -> handler.processSubscribe(ctx, msg, answerCodes, reasonStrings, finalAuthorizersPresent));
            }

        } catch (final Exception e) {
            log.error("Subscription authorization failed: ", e);
        }

    }

    private void disconnectClient(final int topicIndex, final @NotNull SubscriptionAuthorizerOutputImpl output) {
        final String logMessage = "A client (IP: {}) sent a SUBSCRIBE with an unauthorized subscription for topic '" + msg.getTopics().get(topicIndex).getTopic() + "'. This is not allowed. Disconnecting client.";
        final String eventLogMessage = "Sent a SUBSCRIBE with an unauthorized subscription for topic '" + msg.getTopics().get(topicIndex).getTopic() + "'";

        if (ctx.channel().attr(ChannelAttributes.MQTT_VERSION).get() == ProtocolVersion.MQTTv5) {
            mqtt5ServerDisconnector.disconnect(ctx.channel(),
                    logMessage,
                    eventLogMessage,
                    Mqtt5DisconnectReasonCode.from(output.getDisconnectReasonCode()),
                    output.getReasonString());
        } else {
            mqtt3ServerDisconnector.disconnect(ctx.channel(),
                    logMessage,
                    eventLogMessage,
                    null,
                    null);
        }
    }
}
