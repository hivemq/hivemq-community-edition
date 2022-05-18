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
package com.hivemq.mqtt.handler.connect;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.bootstrap.ClientState;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectReasonCode;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
import com.hivemq.extensions.auth.parameter.ModifiableClientSettingsImpl;
import com.hivemq.extensions.events.OnAuthSuccessEvent;
import com.hivemq.extensions.handler.PluginAuthenticatorService;
import com.hivemq.extensions.handler.PluginAuthenticatorServiceImpl;
import com.hivemq.extensions.handler.PluginAuthorizerService;
import com.hivemq.extensions.handler.PluginAuthorizerServiceImpl.AuthorizeWillResultEvent;
import com.hivemq.extensions.handler.tasks.PublishAuthorizerResult;
import com.hivemq.extensions.packets.general.ModifiableDefaultPermissionsImpl;
import com.hivemq.extensions.services.auth.Authorizers;
import com.hivemq.limitation.TopicAliasLimiter;
import com.hivemq.mqtt.handler.KeepAliveDisconnectHandler;
import com.hivemq.mqtt.handler.KeepAliveDisconnectService;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.handler.publish.DefaultPermissionsEvaluator;
import com.hivemq.mqtt.handler.publish.FlowControlHandler;
import com.hivemq.mqtt.handler.publish.PublishFlowHandler;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.connack.CONNACK;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.connect.MqttWillPublish;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.mqtt.services.PublishPollService;
import com.hivemq.persistence.clientsession.ClientSessionPersistence;
import com.hivemq.persistence.clientsession.SharedSubscriptionService;
import com.hivemq.persistence.connection.ConnectionPersistence;
import com.hivemq.util.*;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import static com.hivemq.bootstrap.netty.ChannelHandlerNames.*;
import static com.hivemq.configuration.service.InternalConfigurations.AUTH_DENY_UNAUTHENTICATED_CONNECTIONS;
import static com.hivemq.mqtt.message.connack.CONNACK.KEEP_ALIVE_NOT_SET;
import static com.hivemq.mqtt.message.connack.Mqtt5CONNACK.DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT;

/**
 * The handler which is responsible for CONNECT messages
 */
@Singleton
@ChannelHandler.Sharable
public class ConnectHandler extends SimpleChannelInboundHandler<CONNECT> {

    private static final @NotNull Logger log = LoggerFactory.getLogger(ConnectHandler.class);

    private final @NotNull ClientSessionPersistence clientSessionPersistence;
    private final @NotNull ConnectionPersistence connectionPersistence;
    private final @NotNull FullConfigurationService configurationService;
    private final @NotNull Provider<PublishFlowHandler> publishFlowHandlerProvider;
    private final @NotNull Provider<FlowControlHandler> flowControlHandlerProvider;
    private final @NotNull MqttConnacker mqttConnacker;
    private final @NotNull TopicAliasLimiter topicAliasLimiter;
    private final @NotNull PublishPollService publishPollService;
    private final @NotNull SharedSubscriptionService sharedSubscriptionService;
    private final @NotNull Authorizers authorizers;
    private final @NotNull PluginAuthenticatorService pluginAuthenticatorService;
    private final @NotNull PluginAuthorizerService pluginAuthorizerService;
    private final @NotNull MqttServerDisconnector mqttServerDisconnector;
    private final @NotNull KeepAliveDisconnectService keepAliveDisconnectService;

    private int maxClientIdLength;
    private long configuredSessionExpiryInterval;
    private int topicAliasMaximum;
    private int serverKeepAliveMaximum;
    private boolean allowZeroKeepAlive;
    private long maxMessageExpiryInterval;

    @Inject
    public ConnectHandler(
            final @NotNull ClientSessionPersistence clientSessionPersistence,
            final @NotNull ConnectionPersistence connectionPersistence,
            final @NotNull FullConfigurationService configurationService,
            final @NotNull Provider<PublishFlowHandler> publishFlowHandlerProvider,
            final @NotNull Provider<FlowControlHandler> flowControlHandlerProvider,
            final @NotNull MqttConnacker mqttConnacker,
            final @NotNull TopicAliasLimiter topicAliasLimiter,
            final @NotNull PublishPollService publishPollService,
            final @NotNull SharedSubscriptionService sharedSubscriptionService,
            final @NotNull PluginAuthenticatorService pluginAuthenticatorService,
            final @NotNull Authorizers authorizers,
            final @NotNull PluginAuthorizerService pluginAuthorizerService,
            final @NotNull MqttServerDisconnector mqttServerDisconnector,
            final @NotNull KeepAliveDisconnectService keepAliveDisconnectService) {

        this.clientSessionPersistence = clientSessionPersistence;
        this.connectionPersistence = connectionPersistence;
        this.configurationService = configurationService;
        this.publishFlowHandlerProvider = publishFlowHandlerProvider;
        this.flowControlHandlerProvider = flowControlHandlerProvider;
        this.mqttConnacker = mqttConnacker;
        this.topicAliasLimiter = topicAliasLimiter;
        this.publishPollService = publishPollService;
        this.sharedSubscriptionService = sharedSubscriptionService;
        this.pluginAuthenticatorService = pluginAuthenticatorService;
        this.authorizers = authorizers;
        this.pluginAuthorizerService = pluginAuthorizerService;
        this.mqttServerDisconnector = mqttServerDisconnector;
        this.keepAliveDisconnectService = keepAliveDisconnectService;
    }

    @PostConstruct
    public void postConstruct() {
        maxClientIdLength = configurationService.restrictionsConfiguration().maxClientIdLength();
        configuredSessionExpiryInterval = configurationService.mqttConfiguration().maxSessionExpiryInterval();
        if (configurationService.mqttConfiguration().topicAliasEnabled()) {
            topicAliasMaximum = configurationService.mqttConfiguration().topicAliasMaxPerClient();
        } else {
            topicAliasMaximum = 0;
        }
        serverKeepAliveMaximum = configurationService.mqttConfiguration().keepAliveMax();
        allowZeroKeepAlive = configurationService.mqttConfiguration().keepAliveAllowZero();
        maxMessageExpiryInterval = configurationService.mqttConfiguration().maxMessageExpiryInterval();
    }

    @Override
    protected void channelRead0(final @NotNull ChannelHandlerContext ctx, final @NotNull CONNECT connect)
            throws Exception {
        adjustValuesAccordingToSettings(connect);

        if (!checkClientId(ctx, connect)) {
            return;
        }

        if (!checkWillPublish(ctx, connect)) {
            return;
        }

        if (!checkWillRetained(ctx, connect)) {
            return;
        }

        final ClientConnection clientConnection = ctx.channel().attr(ChannelAttributes.CLIENT_CONNECTION).get();
        clientConnection.setDisconnectFuture(SettableFuture.create());
        clientConnection.setClientReceiveMaximum(connect.getReceiveMaximum());
        //Set max packet size to send to channel
        if (connect.getMaximumPacketSize() <= DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT) {
            clientConnection.setMaxPacketSizeSend(connect.getMaximumPacketSize());
        }

        clientConnection.setRequestResponseInformation(connect.isResponseInformationRequested());
        clientConnection.setRequestProblemInformation(connect.isProblemInformationRequested());

        addPublishFlowHandler(ctx, connect);

        clientConnection.proposeClientState(ClientState.AUTHENTICATING);
        clientConnection.setAuthConnect(connect);
        pluginAuthenticatorService.authenticateConnect(
                ctx,
                clientConnection,
                connect,
                new ModifiableClientSettingsImpl(connect.getReceiveMaximum(), null));
    }

    public void connectSuccessfulUndecided(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull ClientConnection clientConnection,
            final @NotNull CONNECT connect,
            final @Nullable ModifiableClientSettingsImpl clientSettings) {

        if (AUTH_DENY_UNAUTHENTICATED_CONNECTIONS.get()) {
            mqttConnacker.connackError(
                    clientConnection.getChannel(),
                    PluginAuthenticatorServiceImpl.AUTH_FAILED_LOG,
                    ReasonStrings.AUTH_FAILED_NO_AUTHENTICATOR,
                    Mqtt5ConnAckReasonCode.NOT_AUTHORIZED,
                    ReasonStrings.AUTH_FAILED_NO_AUTHENTICATOR,
                    Mqtt5UserProperties.NO_USER_PROPERTIES,
                    true);
            return;
        }

        clientConnection.proposeClientState(ClientState.AUTHENTICATED);
        connectAuthenticated(ctx, clientConnection, connect, clientSettings);
        cleanChannelAttributesAfterAuth(clientConnection);
    }

    public void connectSuccessfulAuthenticated(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull ClientConnection clientConnection,
            final @NotNull CONNECT connect,
            final @Nullable ModifiableClientSettingsImpl clientSettings) {

        clientConnection.proposeClientState(ClientState.AUTHENTICATED);
        connectAuthenticated(ctx, clientConnection, connect, clientSettings);
        cleanChannelAttributesAfterAuth(clientConnection);
    }

    private static void cleanChannelAttributesAfterAuth(final @NotNull ClientConnection clientConnection) {
        final ChannelPipeline pipeline = clientConnection.getChannel().pipeline();
        if (pipeline.context(AUTH_IN_PROGRESS_MESSAGE_HANDLER) != null) {
            try {
                pipeline.remove(AUTH_IN_PROGRESS_MESSAGE_HANDLER);
            } catch (final NoSuchElementException ignored) {
            }
        }
        clientConnection.setAuthConnect(null);
    }

    private void adjustValuesAccordingToSettings(final @NotNull CONNECT connect) {
        if (connect.getWillPublish() != null) {
            final MqttWillPublish willPublish = connect.getWillPublish();
            if (willPublish.getMessageExpiryInterval() > maxMessageExpiryInterval) {
                willPublish.setMessageExpiryInterval(maxMessageExpiryInterval);
            }
        }
    }

    private void addPublishFlowHandler(final @NotNull ChannelHandlerContext ctx, final @NotNull CONNECT connect) {

        ctx.channel()
                .pipeline()
                .addBefore(MESSAGE_EXPIRY_HANDLER, MQTT_PUBLISH_FLOW_HANDLER,
                        publishFlowHandlerProvider.get());

        if (connect.getProtocolVersion() == ProtocolVersion.MQTTv5) {
            ctx.channel()
                    .pipeline()
                    .addBefore(MQTT_MESSAGE_BARRIER, MQTT_5_FLOW_CONTROL_HANDLER,
                            flowControlHandlerProvider.get());
        }
    }

    @Override
    public void userEventTriggered(final @NotNull ChannelHandlerContext ctx, final @NotNull Object evt) throws Exception {
        if (evt instanceof AuthorizeWillResultEvent) {
            final AuthorizeWillResultEvent resultEvent = (AuthorizeWillResultEvent) evt;
            afterPublishAuthorizer(ctx, resultEvent.getConnect(), resultEvent.getResult());
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @NotNull
    private ListenableFuture<Void> updatePersistenceData(
            final boolean cleanStart,
            final @NotNull String clientId,
            final long sessionExpiryInterval,
            final @Nullable MqttWillPublish willPublish,
            final @Nullable Long queueSizeMaximum) {

        return clientSessionPersistence.clientConnected(clientId, cleanStart, sessionExpiryInterval, willPublish, queueSizeMaximum);
    }

    private boolean checkClientId(final @NotNull ChannelHandlerContext ctx, final @NotNull CONNECT msg) {

        final Boolean assigned = ctx.channel().attr(ChannelAttributes.CLIENT_CONNECTION).get().isClientIdAssigned();

        if (assigned != null && assigned) {
            return true;
        }

        if (msg.getClientIdentifier().length() > maxClientIdLength) {

            final String logMessage =
                    "A client (IP: {}) connected with a client identifier longer than " + maxClientIdLength +
                            " characters. This is not allowed.";
            final String eventlogMessage = "Sent CONNECT with Client identifier too long";
            mqttConnacker.connackError(
                    ctx.channel(),
                    logMessage,
                    eventlogMessage,
                    Mqtt5ConnAckReasonCode.CLIENT_IDENTIFIER_NOT_VALID,
                    ReasonStrings.CONNACK_CLIENT_IDENTIFIER_TOO_LONG);
            return false;
        }
        return true;
    }

    private boolean checkWillPublish(final @NotNull ChannelHandlerContext ctx, final @NotNull CONNECT msg) {
        if (msg.getWillPublish() != null) {
            if (Topics.containsWildcard(msg.getWillPublish().getTopic())) {
                mqttConnacker.connackError(
                        ctx.channel(),
                        "A client (IP: {}) sent a CONNECT with a wildcard character in the Will Topic (# or +). This is not allowed.",
                        "Sent CONNECT with wildcard character in the Will Topic (#/+)",
                        Mqtt5ConnAckReasonCode.TOPIC_NAME_INVALID,
                        ReasonStrings.CONNACK_NOT_AUTHORIZED_WILL_WILDCARD);
                return false;

            }

            final int willQos = msg.getWillPublish().getQos().getQosNumber();
            final int maxQos = configurationService.mqttConfiguration().maximumQos().getQosNumber();
            if (willQos > maxQos) {
                mqttConnacker.connackError(
                        ctx.channel(),
                        "A client (IP: {}) sent a CONNECT with a Will QoS higher than the maximum configured QoS. This is not allowed.",
                        "Sent CONNECT with Will QoS (" + willQos + ") higher than the allowed maximum (" + maxQos + ")",
                        Mqtt5ConnAckReasonCode.QOS_NOT_SUPPORTED,
                        String.format(ReasonStrings.CONNACK_QOS_NOT_SUPPORTED_WILL, willQos, maxQos));
                return false;
            }

            final int maxTopicLength = configurationService.restrictionsConfiguration().maxTopicLength();
            if (msg.getWillPublish().getTopic().length() > maxTopicLength) {
                mqttConnacker.connackError(ctx.channel(),
                        "A client (IP: {}) sent a CONNECT with a Will Topic exceeding the max length. This is not allowed.",
                        "Sent CONNECT with Will topic that exceeds maximum topic length",
                        Mqtt5ConnAckReasonCode.TOPIC_NAME_INVALID,
                        ReasonStrings.CONNACK_NOT_AUTHORIZED_MAX_TOPIC_LENGTH_EXCEEDED);
                return false;
            }
        }
        return true;
    }

    private boolean checkWillRetained(final @NotNull ChannelHandlerContext ctx, final @NotNull CONNECT msg) {
        if (msg.getWillPublish() != null && msg.getWillPublish().isRetain() &&
                !configurationService.mqttConfiguration().retainedMessagesEnabled()) {
            mqttConnacker.connackError(
                    ctx.channel(),
                    "A client (IP: {}) sent a CONNECT with Will Retain set to 1 although retain is not available.",
                    "Sent a CONNECT with Will Retain set to 1 although retain is not available",
                    Mqtt5ConnAckReasonCode.RETAIN_NOT_SUPPORTED,
                    ReasonStrings.CONNACK_RETAIN_NOT_SUPPORTED);
            return false;
        }
        return true;
    }

    private void connectAuthenticated(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull ClientConnection clientConnection,
            final @NotNull CONNECT msg,
            final @Nullable ModifiableClientSettingsImpl clientSettings) {

        clientConnection.setPreventLwt(true); //do not send will until it is authorized

        if (clientSettings != null && clientSettings.isModified()) {
            applyClientSettings(clientSettings, msg, clientConnection.getChannel());
        }

        if (msg.getWillPublish() != null) {
            if (authorizers.areAuthorizersAvailable()) {
                ctx.executor().execute(() -> pluginAuthorizerService.authorizeWillPublish(ctx, msg));
            } else {
                if (isWillNotAuthorized(ctx, msg)) {
                    return;
                }
                continueAfterWillAuthorization(ctx, clientConnection, msg);
            }
        } else {
            continueAfterWillAuthorization(ctx, clientConnection, msg);
        }
    }

    private void applyClientSettings(final @NotNull ModifiableClientSettingsImpl clientSettings,
                                     final @NotNull CONNECT msg,
                                     @NotNull final Channel channel) {
        msg.setReceiveMaximum(clientSettings.getClientReceiveMaximum());

        final ClientConnection clientConnection = channel.attr(ChannelAttributes.CLIENT_CONNECTION).get();
        clientConnection.setClientReceiveMaximum(clientSettings.getClientReceiveMaximum());
        clientConnection.setQueueSizeMaximum(clientSettings.getQueueSizeMaximum());
    }

    private void continueAfterWillAuthorization(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull ClientConnection clientConnection,
            final @NotNull CONNECT msg) {

        clientConnection.getChannel().pipeline().fireUserEventTriggered(new OnAuthSuccessEvent());

        disconnectClientWithSameClientId(clientConnection, ctx, msg);
    }

    private void afterPublishAuthorizer(@NotNull final ChannelHandlerContext ctx, @NotNull final CONNECT msg, @NotNull final PublishAuthorizerResult authorizerResult) {

        final ClientConnection clientConnection = ctx.channel().attr(ChannelAttributes.CLIENT_CONNECTION).get();

        if (authorizerResult.isAuthorizerPresent() && authorizerResult.getAckReasonCode() != null) {
            //decision has been made in PublishAuthorizer
            if (authorizerResult.getAckReasonCode() == AckReasonCode.SUCCESS) {
                continueAfterWillAuthorization(ctx, clientConnection, msg);
            } else {
                connackWillNotAuthorized(ctx, msg, authorizerResult.getDisconnectReasonCode(), authorizerResult.getAckReasonCode(), authorizerResult.getReasonString());
            }
            return;
        }

        final ModifiableDefaultPermissions permissions = clientConnection.getAuthPermissions();
        final ModifiableDefaultPermissionsImpl defaultPermissions = (ModifiableDefaultPermissionsImpl) permissions;

        //if authorizers are present and no permissions are available and the default behaviour has not been changed
        //then we deny the publish
        if (authorizerResult.isAuthorizerPresent()
                && (defaultPermissions == null || (defaultPermissions.asList().size() < 1
                && !defaultPermissions.isDefaultAuthorizationBehaviourOverridden()))) {

            connackWillNotAuthorized(ctx, msg, authorizerResult.getDisconnectReasonCode(), null, null);
            return;
        }

        if (!DefaultPermissionsEvaluator.checkWillPublish(permissions, msg.getWillPublish())) {
            //will is not authorized, disconnect client
            connackWillNotAuthorized(ctx, msg, authorizerResult.getDisconnectReasonCode(), authorizerResult.getAckReasonCode(), authorizerResult.getReasonString());
            return;
        }

        continueAfterWillAuthorization(ctx, clientConnection, msg);
    }

    private boolean isWillNotAuthorized(@NotNull final ChannelHandlerContext ctx, @NotNull final CONNECT msg) {
        if (msg.getWillPublish() != null) {
            final ModifiableDefaultPermissions permissions = ctx.channel().attr(ChannelAttributes.CLIENT_CONNECTION).get().getAuthPermissions();
            if (!DefaultPermissionsEvaluator.checkWillPublish(permissions, msg.getWillPublish())) {

                //will is not authorized, disconnect client
                connackWillNotAuthorized(ctx, msg, null, null, null);

                return true;
            }
        }
        return false;
    }

    private void connackWillNotAuthorized(@NotNull final ChannelHandlerContext ctx, @NotNull final CONNECT msg,
                                          @Nullable final DisconnectReasonCode disconnectReasonCode,
                                          @Nullable final AckReasonCode ackReasonCode, @Nullable final String reasonString) {

        Mqtt5ConnAckReasonCode connAckReasonCode = disconnectReasonCode != null ?
                Mqtt5ConnAckReasonCode.fromDisconnectReasonCode(disconnectReasonCode) : null;

        if (connAckReasonCode == null) {
            connAckReasonCode = ackReasonCode != null ?
                    Mqtt5ConnAckReasonCode.fromAckReasonCode(ackReasonCode) : Mqtt5ConnAckReasonCode.NOT_AUTHORIZED;
        }

        final String usedReasonString = reasonString != null ? reasonString : "Will Publish is not authorized for topic '"
                + msg.getWillPublish().getTopic() + "' with QoS '" + msg.getWillPublish().getQos()
                + "' and retain '" + msg.getWillPublish().isRetain() + "'";

        mqttConnacker.connackError(
                ctx.channel(),
                "A client (IP: {}) sent a CONNECT message with an not authorized Will Publish to topic '"
                        + msg.getWillPublish().getTopic() + "' with QoS '" + msg.getWillPublish().getQos().getQosNumber()
                        + "' and retain '" + msg.getWillPublish().isRetain() + "'.",
                "Sent a CONNECT message with an not authorized Will Publish to topic '" +
                        msg.getWillPublish().getTopic() + "' with QoS '" + msg.getWillPublish().getQos().getQosNumber()
                        + "' and retain '" + msg.getWillPublish().isRetain() + "'",
                connAckReasonCode,
                usedReasonString,
                Mqtt5UserProperties.NO_USER_PROPERTIES,
                true);
    }

    @VisibleForTesting
    void afterTakeover(final @NotNull ChannelHandlerContext ctx, final @NotNull CONNECT msg) {

        final Long queueSizeMaximum = ctx.channel().attr(ChannelAttributes.CLIENT_CONNECTION).get().getQueueSizeMaximum();
        final long sessionExpiryInterval =
                msg.getSessionExpiryInterval() > configuredSessionExpiryInterval ?
                        configuredSessionExpiryInterval : msg.getSessionExpiryInterval();

        final boolean existent;
        if (msg.isCleanStart()) {
            existent = false;
        } else {
            existent = clientSessionPersistence.isExistent(msg.getClientIdentifier());
        }
        final ListenableFuture<Void> future = updatePersistenceData(msg.isCleanStart(),
                msg.getClientIdentifier(), sessionExpiryInterval, msg.getWillPublish(),
                queueSizeMaximum);

        Futures.addCallback(future, new UpdatePersistenceCallback(ctx, this, msg, existent), ctx.executor());
    }

    private void afterPersistSession(final @NotNull ChannelHandlerContext ctx, final @NotNull CONNECT msg, final boolean sessionPresent) {

        // In case the clients session expired while it was disconnected, the cache will be invalidated before the client connects.
        // This is sufficient since messages for shared subscriptions are not queued for specific clients.
        sharedSubscriptionService.invalidateSharedSubscriptionCache(msg.getClientIdentifier());

        addKeepAliveHandler(ctx, msg);
        sendConnackSuccess(ctx, msg, sessionPresent);

        //We're removing ourselves
        try {
            ctx.pipeline().remove(this);
        } catch (final NoSuchElementException e) {
            //noop since handler has already been removed
        }
    }

    private void sendConnackSuccess(final @NotNull ChannelHandlerContext ctx, final @NotNull CONNECT msg, final boolean sessionPresent) {

        final ClientConnection clientConnection = ctx.channel().attr(ChannelAttributes.CLIENT_CONNECTION).get();

        final ChannelFuture connackSent;

        clientConnection.setConnectMessage(msg);

        if (msg.getProtocolVersion() == ProtocolVersion.MQTTv5) {
            final CONNACK connack = buildMqtt5Connack(ctx.channel(), msg, sessionPresent);
            connackSent = mqttConnacker.connackSuccess(ctx, connack);
        } else {
            clientConnection.setClientSessionExpiryInterval(msg.getSessionExpiryInterval());
            if (sessionPresent) {
                connackSent = mqttConnacker.connackSuccess(ctx, ConnackMessages.ACCEPTED_MSG_SESS_PRESENT);
            } else {
                connackSent = mqttConnacker.connackSuccess(ctx, ConnackMessages.ACCEPTED_MSG_NO_SESS);
            }
        }

        //send out queued messages (from inflight and client-session queue) for client after connack is sent
        connackSent.addListener(new PollInflightMessageListener(publishPollService, clientConnection.getClientId()));
    }

    private @NotNull CONNACK buildMqtt5Connack(final @NotNull Channel channel, final @NotNull CONNECT msg, final boolean sessionPresent) {
        final CONNACK.Mqtt5Builder builder = new CONNACK.Mqtt5Builder()
                .withSessionPresent(sessionPresent)
                .withReasonCode(Mqtt5ConnAckReasonCode.SUCCESS)
                .withReceiveMaximum(configurationService.mqttConfiguration().serverReceiveMaximum())
                .withSubscriptionIdentifierAvailable(configurationService.mqttConfiguration().subscriptionIdentifierEnabled())
                .withMaximumPacketSize(configurationService.mqttConfiguration().maxPacketSize())
                .withWildcardSubscriptionAvailable(configurationService.mqttConfiguration().wildcardSubscriptionsEnabled())
                .withSharedSubscriptionAvailable(configurationService.mqttConfiguration().sharedSubscriptionsEnabled())
                .withMaximumQoS(configurationService.mqttConfiguration().maximumQos())
                .withRetainAvailable(configurationService.mqttConfiguration().retainedMessagesEnabled());

        final boolean overridden = msg.getSessionExpiryInterval() > configuredSessionExpiryInterval;
        final long sessionExpiryInterval = overridden ? configuredSessionExpiryInterval : msg.getSessionExpiryInterval();

        if (overridden) {
            builder.withSessionExpiryInterval(sessionExpiryInterval);
        }

        final ClientConnection clientConnection = channel.attr(ChannelAttributes.CLIENT_CONNECTION).get();

        //when client identifier assigned, send it in CONNACK
        final boolean clientIdAssigned = clientConnection.isClientIdAssigned();
        if (clientIdAssigned) {
            builder.withAssignedClientIdentifier(msg.getClientIdentifier());
        }

        //send server keep alive max when connect keep alive is zero and zero is not allowed or keep alive > server keep alive maximum
        if ((msg.getKeepAlive() == 0 && !allowZeroKeepAlive) || (msg.getKeepAlive() > serverKeepAliveMaximum)) {
            builder.withServerKeepAlive(serverKeepAliveMaximum);
            clientConnection.setConnectKeepAlive(serverKeepAliveMaximum);
        } else {
            builder.withServerKeepAlive(KEEP_ALIVE_NOT_SET);
            clientConnection.setConnectKeepAlive(msg.getKeepAlive());
        }

        //init Topic Alias Mapping if maximum is greater than zero and aliases are available
        if (topicAliasMaximum > 0 && topicAliasLimiter.aliasesAvailable()) {
            clientConnection.setTopicAliasMapping(new String[topicAliasMaximum]);
            builder.withTopicAliasMaximum(topicAliasMaximum);
            topicAliasLimiter.initUsage(topicAliasMaximum);
        }

        //Set session expiry interval to channel for DISCONNECT
        clientConnection.setClientSessionExpiryInterval(sessionExpiryInterval);

        //set userproperties from auth to connack
        final Mqtt5UserProperties userPropertiesFromAuth = clientConnection.getAuthUserProperties();
        if (userPropertiesFromAuth != null) {
            clientConnection.setAuthUserProperties(null);
            builder.withUserProperties(userPropertiesFromAuth);
        }

        // set auth method if present
        final String authMethod = clientConnection.getAuthMethod();
        if (authMethod != null) {
            builder.withAuthMethod(authMethod);

            // set auth data
            final ByteBuffer authData = clientConnection.getAuthData();
            if (authData != null) {
                clientConnection.setAuthData(null);
                builder.withAuthData(Bytes.fromReadOnlyBuffer(authData));
            }
        }

        return builder.build();
    }

    private void disconnectClientWithSameClientId(
            final @NotNull ClientConnection clientConnection,
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull CONNECT msg) {

        if (clientConnection.getClientState().disconnected()) {
            log.debug("Disconnecting client with same client identifier '{}' failed. " +
                    "Cause: Disconnected before takeover.", clientConnection.getClientId());
            return;
        }

        final ClientConnection persistedClientConnection = connectionPersistence.persistIfAbsent(clientConnection);
        // We have written our ClientConnection to the ConnectionPersistence. We are now able to connect.
        if (persistedClientConnection == clientConnection) {
            afterTakeover(ctx, msg);
            return;
        }

        // It is ok that multiple clients can queue a task here as we guard the client with the check disconnectingOrDisconnected().
        persistedClientConnection.getChannel().eventLoop().execute(() -> {
            if (!persistedClientConnection.getClientState().disconnectingOrDisconnected()) {
                mqttServerDisconnector.disconnect(persistedClientConnection.getChannel(),
                        "Disconnecting already connected client with id {} and ip {} because another client connects with that id",
                        ReasonStrings.DISCONNECT_SESSION_TAKEN_OVER,
                        Mqtt5DisconnectReasonCode.SESSION_TAKEN_OVER,
                        ReasonStrings.DISCONNECT_SESSION_TAKEN_OVER);
            }
        });

        Futures.addCallback(persistedClientConnection.getDisconnectFuture(), new FutureCallback<>() {
            @Override
            public void onSuccess(final Void result) {
                disconnectClientWithSameClientId(clientConnection, ctx, msg);
            }

            @Override
            public void onFailure(final @NotNull Throwable t) {
                log.warn("Exception on disconnecting client with same client identifier '{}'. Cause: {}",
                        clientConnection.getClientId(),
                        t.getMessage());
            }
        }, clientConnection.getChannel().eventLoop());
    }

    private void addKeepAliveHandler(final @NotNull ChannelHandlerContext ctx, final @NotNull CONNECT msg) {

        final int keepAlive;
        if (ProtocolVersion.MQTTv5.equals(msg.getProtocolVersion()) &&
                ((msg.getKeepAlive() == 0 && !allowZeroKeepAlive) || (msg.getKeepAlive() > serverKeepAliveMaximum))) {
            if (log.isTraceEnabled()) {
                log.trace("Client {} used keepAlive {} which is invalid, using server maximum of {}", msg.getClientIdentifier(), msg.getKeepAlive(), serverKeepAliveMaximum);
            }
            keepAlive = serverKeepAliveMaximum;
        } else {
            keepAlive = msg.getKeepAlive();
        }

        if (keepAlive > 0) {

            // The MQTT spec defines a 1.5 grace period
            final Double keepAliveValue = keepAlive * getGracePeriod();
            if (log.isTraceEnabled()) {
                log.trace("Client {} specified a keepAlive value of {}s. Using keepAlive of {}s. The maximum timeout before disconnecting is {}s",
                        msg.getClientIdentifier(), msg.getKeepAlive(), keepAlive, keepAliveValue);
            }
            ctx.pipeline().addFirst(MQTT_KEEPALIVE_IDLE_HANDLER, new KeepAliveDisconnectHandler(keepAliveValue.intValue(), TimeUnit.SECONDS, keepAliveDisconnectService));
        } else {
            if (log.isTraceEnabled()) {
                log.trace("Client {} specified keepAlive of 0. Disabling PING mechanism", msg.getClientIdentifier());
            }
        }
    }

    private static double getGracePeriod() {
        return InternalConfigurations.MQTT_CONNECTION_KEEP_ALIVE_FACTOR;
    }

    private static final class UpdatePersistenceCallback implements FutureCallback<Void> {
        private final @NotNull ChannelHandlerContext ctx;
        private final @NotNull ConnectHandler connectHandler;
        private final @NotNull CONNECT connect;
        private final boolean sessionPresent;

        private UpdatePersistenceCallback(
                final @NotNull ChannelHandlerContext ctx,
                final @NotNull ConnectHandler connectHandler,
                final @NotNull CONNECT connect,
                final boolean sessionPresent) {
            this.ctx = ctx;
            this.connectHandler = connectHandler;
            this.connect = connect;
            this.sessionPresent = sessionPresent;
        }

        @Override
        public void onSuccess(final @Nullable Void aVoid) {
            if (ctx.channel().isActive() && !ctx.executor().isShutdown()) {
                connectHandler.afterPersistSession(ctx, connect, sessionPresent);
            }
        }

        @Override
        public void onFailure(final @NotNull Throwable throwable) {
            Exceptions.rethrowError("Unable to handle client connection for id " + connect.getClientIdentifier() + ".", throwable);
            ctx.channel().disconnect();
        }
    }
}
