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
package com.hivemq.mqtt.handler.publish;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.configuration.service.RestrictionsConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.auth.DefaultAuthorizationBehaviour;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
import com.hivemq.extensions.handler.tasks.PublishAuthorizerResult;
import com.hivemq.extensions.packets.general.ModifiableDefaultPermissionsImpl;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.pubrec.PUBREC;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5PubAckReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5PubRecReasonCode;
import com.hivemq.mqtt.services.InternalPublishService;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.ChannelUtils;
import com.hivemq.util.ReasonStrings;
import io.netty.channel.ChannelHandlerContext;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.hivemq.util.ChannelAttributes.MQTT_VERSION;

/**
 * This Service is responsible for PUBLISH message processing after interception and authorisation.
 *
 * @author Dominik Obermaier
 * @author Christoph Schäbel
 * @author Florian Limpöck
 */
@Singleton
public class IncomingPublishService {

    private final @NotNull InternalPublishService publishService;
    private final @NotNull MqttConfigurationService mqttConfigurationService;
    private final @NotNull RestrictionsConfigurationService restrictionsConfigurationService;
    private final @NotNull MqttServerDisconnector mqttServerDisconnector;

    @Inject
    IncomingPublishService(final @NotNull InternalPublishService publishService,
                           final @NotNull MqttConfigurationService mqttConfigurationService,
                           final @NotNull RestrictionsConfigurationService restrictionsConfigurationService,
                           final @NotNull MqttServerDisconnector mqttServerDisconnector) {

        this.publishService = publishService;
        this.mqttConfigurationService = mqttConfigurationService;
        this.restrictionsConfigurationService = restrictionsConfigurationService;
        this.mqttServerDisconnector = mqttServerDisconnector;
    }

    public void processPublish(@NotNull final ChannelHandlerContext ctx,
                               @NotNull final PUBLISH publish,
                               @Nullable final PublishAuthorizerResult authorizerResult) {

        final ProtocolVersion protocolVersion = ctx.channel().attr(MQTT_VERSION).get();

        final int maxQos = mqttConfigurationService.maximumQos().getQosNumber();
        final int qos = publish.getQoS().getQosNumber();
        if (qos > maxQos) {
            final String clientId = ChannelUtils.getClientId(ctx.channel());
            mqttServerDisconnector.disconnect(ctx.channel(),
                    "Client '" + clientId + "' (IP: {}) sent a PUBLISH with QoS exceeding the maximum configured QoS." +
                            " Got QoS " + publish.getQoS() + ", maximum: " + mqttConfigurationService.maximumQos() + ". Disconnecting client.",
                    "Sent PUBLISH with QoS (" + qos + ") higher than the allowed maximum (" + maxQos + ")",
                    Mqtt5DisconnectReasonCode.QOS_NOT_SUPPORTED,
                    String.format(ReasonStrings.CONNACK_QOS_NOT_SUPPORTED_PUBLISH, qos, maxQos)
            );
            return;
        }

        final String topic = publish.getTopic();
        final int maxTopicLength = restrictionsConfigurationService.maxTopicLength();
        if (topic.length() > maxTopicLength) {
            final String clientId = ChannelUtils.getClientId(ctx.channel());
            final String logMessage = "Client '" + clientId + "' (IP: {}) sent a PUBLISH with a topic that exceeds the maximum configured length of '" + maxTopicLength + "' . Disconnecting client.";
            mqttServerDisconnector.disconnect(ctx.channel(),
                    logMessage,
                    "Sent PUBLISH for a topic that exceeds maximum topic length",
                    Mqtt5DisconnectReasonCode.TOPIC_NAME_INVALID,
                    ReasonStrings.DISCONNECT_MAXIMUM_TOPIC_LENGTH_EXCEEDED);
            return;
        }

        if (ProtocolVersion.MQTTv3_1 == protocolVersion || ProtocolVersion.MQTTv3_1_1 == protocolVersion) {
            final Long maxPublishSize = ctx.channel().attr(ChannelAttributes.MAX_PACKET_SIZE_SEND).get();
            if (!isMessageSizeAllowed(maxPublishSize, publish)) {
                final String clientId = ChannelUtils.getClientId(ctx.channel());
                final String logMessage = "Client '" + clientId + "' (IP: {}) sent a PUBLISH with " + publish.getPayload().length + " bytes payload its max allowed size is " + maxPublishSize + " bytes. Disconnecting client.";
                final String reason = "Sent PUBLISH with a payload that is bigger than the allowed message size";
                mqttServerDisconnector.disconnect(ctx.channel(),
                        logMessage,
                        reason,
                        Mqtt5DisconnectReasonCode.PACKET_TOO_LARGE,
                        reason);
                return;
            }
        }

        authorizePublish(ctx, publish, authorizerResult);
    }

    private void authorizePublish(@NotNull final ChannelHandlerContext ctx, @NotNull final PUBLISH publish, @Nullable final PublishAuthorizerResult authorizerResult) {

        if (authorizerResult != null && authorizerResult.getAckReasonCode() != null) {
            //decision has been made in PublishAuthorizer
            if (ctx.channel().attr(ChannelAttributes.INCOMING_PUBLISHES_DEFAULT_FAILED_SKIP_REST).get() != null) {
                //reason string and reason code null, because client disconnected previously
                finishUnauthorizedPublish(ctx, publish, null, null);
            } else if (authorizerResult.getAckReasonCode() == AckReasonCode.SUCCESS) {
                publishMessage(ctx, publish);
            } else {
                finishUnauthorizedPublish(ctx, publish, authorizerResult.getAckReasonCode(), authorizerResult.getReasonString());
            }
            return;
        }

        final ModifiableDefaultPermissions permissions = ctx.channel().attr(ChannelAttributes.AUTH_PERMISSIONS).get();
        final ModifiableDefaultPermissionsImpl defaultPermissions = (ModifiableDefaultPermissionsImpl) permissions;

        //if authorizers are present and no permissions are available and the default behaviour has not been changed
        //then we deny the publish
        if (authorizerResult != null && authorizerResult.isAuthorizerPresent()
                && (defaultPermissions == null || (defaultPermissions.asList().size() < 1
                && defaultPermissions.getDefaultBehaviour() == DefaultAuthorizationBehaviour.ALLOW
                && !defaultPermissions.isDefaultAuthorizationBehaviourOverridden()))) {
            finishUnauthorizedPublish(ctx, publish, null, null);
            return;
        }

        if (DefaultPermissionsEvaluator.checkPublish(permissions, publish)) {
            publishMessage(ctx, publish);
        } else {
            finishUnauthorizedPublish(ctx, publish, null, null);
        }
    }

    private void finishUnauthorizedPublish(@NotNull final ChannelHandlerContext ctx, @NotNull final PUBLISH publish,
                                           @Nullable final AckReasonCode reasonCode, @Nullable final String reasonString) {

        ctx.channel().attr(ChannelAttributes.INCOMING_PUBLISHES_DEFAULT_FAILED_SKIP_REST).set(true);

        if (!ctx.channel().isActive()) {
            //no more processing needed.
            return;
        }

        final String reason = "Not authorized to publish on topic '" + publish.getTopic() + "' with QoS '"
                + publish.getQoS().getQosNumber() + "' and retain '" + publish.isRetain() + "'";

        //MQTT 3.x.x -> disconnect (without publish answer packet)
        if (ctx.channel().attr(ChannelAttributes.MQTT_VERSION).get() != ProtocolVersion.MQTTv5) {

            final String clientId = ChannelUtils.getClientId(ctx.channel());
            mqttServerDisconnector.disconnect(ctx.channel(),
                    "Client '" + clientId + "' (IP: {}) is not authorized to publish on topic '" + publish.getTopic()
                            + "' with QoS '" + publish.getQoS().getQosNumber() + "' and retain '" + publish.isRetain()
                            + "'. Disconnecting client.",
                    reason,
                    Mqtt5DisconnectReasonCode.NOT_AUTHORIZED,
                    reason
            );
            return;
        }

        //MQTT 5 -> send ACK with error code and then disconnect
        switch (publish.getQoS()) {
            case AT_MOST_ONCE:
                //just drop the message, no back channel to the client
                break;
            case AT_LEAST_ONCE:
                final PUBACK puback = new PUBACK(publish.getPacketIdentifier(),
                        reasonCode != null ? Mqtt5PubAckReasonCode.from(reasonCode) : Mqtt5PubAckReasonCode.NOT_AUTHORIZED,
                        reasonString != null ? reasonString : reason, Mqtt5UserProperties.NO_USER_PROPERTIES);
                ctx.pipeline().writeAndFlush(puback);
                break;
            case EXACTLY_ONCE:
                final PUBREC pubrec = new PUBREC(publish.getPacketIdentifier(),
                        reasonCode != null ? Mqtt5PubRecReasonCode.from(reasonCode) : Mqtt5PubRecReasonCode.NOT_AUTHORIZED,
                        reasonString != null ? reasonString : reason, Mqtt5UserProperties.NO_USER_PROPERTIES);
                ctx.pipeline().writeAndFlush(pubrec);
                break;
        }

        final String clientId = ChannelUtils.getClientId(ctx.channel());
        mqttServerDisconnector.disconnect(ctx.channel(),
                "Client '" + clientId + "' (IP: {}) is not authorized to publish on topic '" + publish.getTopic()
                        + "' with QoS '" + publish.getQoS().getQosNumber() + "' and retain '" + publish.isRetain()
                        + "'. Disconnecting client.",
                reason,
                Mqtt5DisconnectReasonCode.NOT_AUTHORIZED,
                reason
        );
    }

    private void publishMessage(final ChannelHandlerContext ctx, @NotNull final PUBLISH publish) {

        final String clientId = ChannelUtils.getClientId(ctx.channel());
        final ListenableFuture<PublishReturnCode> publishFinishedFuture = publishService.publish(publish, ctx.channel().eventLoop(), clientId);
        Futures.addCallback(publishFinishedFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable final PublishReturnCode result) {
                sendAck(ctx, publish, result);
            }

            @Override
            public void onFailure(@NotNull final Throwable t) {
                sendAck(ctx, publish, PublishReturnCode.FAILED);
            }
        }, ctx.channel().eventLoop());
    }

    private void sendAck(@NotNull final ChannelHandlerContext ctx, final PUBLISH publish, @Nullable final PublishReturnCode publishReturnCode) {

        switch (publish.getQoS()) {
            case AT_MOST_ONCE:
                // do nothing
                break;
            case AT_LEAST_ONCE:
                if (publishReturnCode == PublishReturnCode.NO_MATCHING_SUBSCRIBERS) {
                    ctx.pipeline().writeAndFlush(new PUBACK(publish.getPacketIdentifier(), Mqtt5PubAckReasonCode.NO_MATCHING_SUBSCRIBERS,
                            null, Mqtt5UserProperties.NO_USER_PROPERTIES));
                } else {
                    ctx.pipeline().writeAndFlush(new PUBACK(publish.getPacketIdentifier()));
                }
                break;
            case EXACTLY_ONCE:
                if (publishReturnCode == PublishReturnCode.NO_MATCHING_SUBSCRIBERS) {
                    ctx.pipeline().writeAndFlush(new PUBREC(publish.getPacketIdentifier(), Mqtt5PubRecReasonCode.NO_MATCHING_SUBSCRIBERS,
                            null, Mqtt5UserProperties.NO_USER_PROPERTIES));
                } else {
                    ctx.pipeline().writeAndFlush(new PUBREC(publish.getPacketIdentifier()));
                }
                break;
        }
    }

    private boolean isMessageSizeAllowed(final @Nullable Long maxPublishSize, @NotNull final PUBLISH publish) {
        return maxPublishSize == null || publish.getPayload() == null || maxPublishSize >= publish.getPayload().length;
    }


}
