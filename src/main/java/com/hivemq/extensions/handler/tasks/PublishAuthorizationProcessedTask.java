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

import com.google.common.util.concurrent.FutureCallback;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
import com.hivemq.extensions.auth.parameter.PublishAuthorizerOutputImpl;
import com.hivemq.mqtt.handler.disconnect.Mqtt3ServerDisconnector;
import com.hivemq.mqtt.handler.disconnect.Mqtt5ServerDisconnector;
import com.hivemq.mqtt.handler.publish.IncomingPublishService;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.Exceptions;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author Christoph Schäbel
 */
public class PublishAuthorizationProcessedTask implements FutureCallback<PublishAuthorizerOutputImpl> {

    private final @NotNull PUBLISH publish;
    private final @NotNull ChannelHandlerContext ctx;
    private final @NotNull Mqtt5ServerDisconnector mqtt5ServerDisconnector;
    private final @NotNull Mqtt3ServerDisconnector mqtt3ServerDisconnector;
    private final @NotNull IncomingPublishService incomingPublishService;

    public PublishAuthorizationProcessedTask(
            final @NotNull PUBLISH publish,
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull Mqtt5ServerDisconnector mqtt5ServerDisconnector,
            final @NotNull Mqtt3ServerDisconnector mqtt3ServerDisconnector,
            final @NotNull IncomingPublishService incomingPublishService) {

        this.publish = publish;
        this.ctx = ctx;
        this.mqtt5ServerDisconnector = mqtt5ServerDisconnector;
        this.mqtt3ServerDisconnector = mqtt3ServerDisconnector;
        this.incomingPublishService = incomingPublishService;
    }

    @Override
    public void onSuccess(@Nullable final PublishAuthorizerOutputImpl output) {
        if (output == null) {
            //this does not happen
            return;
        }

        AckReasonCode reasonCode = null;
        String reasonString = null;

        switch (output.getAuthorizationState()) {
            case DISCONNECT:
                disconnectClient(output);
                return;
            case FAIL:
                reasonCode = output.getAckReasonCode() != null ? output.getAckReasonCode() : AckReasonCode.NOT_AUTHORIZED;
                reasonString = output.getReasonString() != null ? output.getReasonString() : getReasonString(publish);
                break;
            case UNDECIDED:
                if (!output.isAuthorizerPresent()) {
                    //providers never returned an authorizer, same as continue
                    break;
                }
                reasonCode = AckReasonCode.NOT_AUTHORIZED;
                reasonString = getReasonString(publish);
                break;
            case SUCCESS:
                reasonCode = AckReasonCode.SUCCESS;
                break;
            case CONTINUE:
                break;
            default:
                //no state left
                throw new IllegalStateException("Unknown type");
        }

        //call method in IncomingPublishService with additional info
        final AckReasonCode finalReasonCode = reasonCode;
        final String finalReasonString = reasonString;
        ctx.executor().execute(() -> {
            incomingPublishService.processPublish(ctx, publish, new PublishAuthorizerResult(finalReasonCode, finalReasonString, output.isAuthorizerPresent(), output.getDisconnectReasonCode()));
        });
    }

    @Override
    public void onFailure(@NotNull final Throwable t) {
        Exceptions.rethrowError("Exception at PublishAuthorization", t);
        disconnectClient(null);
    }

    private void disconnectClient(@Nullable final PublishAuthorizerOutputImpl output) {
        final String logMessage = "A client (IP: {}) sent a PUBLISH to an unauthorized topic '" + publish.getTopic() + "'. Disconnecting client from extension.";
        final String eventLogMessage = "Sent a PUBLISH to an unauthorized topic '" + publish.getTopic() + "', extension requested disconnect";

        if (ctx.channel().attr(ChannelAttributes.MQTT_VERSION).get() == ProtocolVersion.MQTTv5) {
            mqtt5ServerDisconnector.disconnect(ctx.channel(),
                    logMessage,
                    eventLogMessage,
                    output != null ? Mqtt5DisconnectReasonCode.from(output.getDisconnectReasonCode()) : Mqtt5DisconnectReasonCode.NOT_AUTHORIZED,
                    output != null ? output.getReasonString() : null);
        } else {
            mqtt3ServerDisconnector.disconnect(ctx.channel(),
                    logMessage,
                    eventLogMessage,
                    null,
                    null);
        }
    }

    private String getReasonString(@NotNull final PUBLISH publish) {
        return "Not authorized to publish on topic '" + publish.getTopic() + "' with QoS '"
                + publish.getQoS().getQosNumber() + "' and retain '" + publish.isRetain() + "'";
    }
}
