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
package com.hivemq.extensions.handler;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.parameter.AuthorizerProviderInput;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extension.sdk.api.services.auth.provider.AuthorizerProvider;
import com.hivemq.extensions.ExtensionPriorityComparator;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.auth.parameter.*;
import com.hivemq.extensions.client.ClientAuthorizers;
import com.hivemq.extensions.client.ClientAuthorizersImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.handler.tasks.*;
import com.hivemq.extensions.packets.general.UserPropertiesImpl;
import com.hivemq.extensions.services.auth.Authorizers;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.handler.publish.IncomingPublishService;
import com.hivemq.mqtt.handler.subscribe.IncomingSubscribeService;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.mqtt.message.subscribe.SUBSCRIBE;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.util.Topics;
import io.netty.channel.ChannelHandlerContext;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.hivemq.configuration.service.InternalConfigurations.MQTT_ALLOW_DOLLAR_TOPICS;

/**
 * @author Florian Limpöck
 * @since 4.1.0
 */
@Singleton
public class PluginAuthorizerServiceImpl implements PluginAuthorizerService {

    private final @NotNull Authorizers authorizers;
    private final @NotNull PluginOutPutAsyncer asyncer;
    private final @NotNull PluginTaskExecutorService pluginTaskExecutorService;
    private final @NotNull ServerInformation serverInformation;
    private final @NotNull MqttServerDisconnector mqttServerDisconnector;
    private final @NotNull ExtensionPriorityComparator extensionPriorityComparator;
    private final @NotNull IncomingPublishService incomingPublishService;
    private final @NotNull IncomingSubscribeService incomingSubscribeService;

    private final boolean allowDollarTopics;

    @Inject
    public PluginAuthorizerServiceImpl(
            final @NotNull Authorizers authorizers,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull PluginTaskExecutorService pluginTaskExecutorService,
            final @NotNull ServerInformation serverInformation,
            final @NotNull HiveMQExtensions hiveMQExtensions,
            final @NotNull MqttServerDisconnector mqttServerDisconnector,
            final @NotNull IncomingPublishService incomingPublishService,
            final @NotNull IncomingSubscribeService incomingSubscribeService) {

        this.authorizers = authorizers;
        this.asyncer = asyncer;
        this.pluginTaskExecutorService = pluginTaskExecutorService;
        this.serverInformation = serverInformation;
        this.incomingPublishService = incomingPublishService;
        this.mqttServerDisconnector = mqttServerDisconnector;
        this.extensionPriorityComparator = new ExtensionPriorityComparator(hiveMQExtensions);
        this.incomingSubscribeService = incomingSubscribeService;
        this.allowDollarTopics = MQTT_ALLOW_DOLLAR_TOPICS.get();
    }

    public void authorizePublish(final @NotNull ChannelHandlerContext ctx, final @NotNull PUBLISH msg) {

        //We first check if the topic is allowed to be published
        if (!Topics.isValidTopicToPublish(msg.getTopic())) {
            disconnectWithReasonCode(ctx, "an invalid topic ('" + msg.getTopic() + "')", "an invalid topic");
            return;
        }

        // if $ topics are not allowed
        if (!allowDollarTopics && Topics.isDollarTopic(msg.getTopic())) {
            final String reason = "a topic that starts with '$'";
            disconnectWithReasonCode(ctx, reason + " ('" + msg.getTopic() + "')", reason);
            return;
        }


        final String clientId = ctx.channel().attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().getClientId();

        if (clientId == null) {
            //we must process the msg in every case !
            incomingPublishService.processPublish(ctx, msg, null);
            return;
        }

        if (!authorizers.areAuthorizersAvailable()) {
            incomingPublishService.processPublish(ctx, msg, null);
            return;
        }

        final Map<String, AuthorizerProvider> providerMap = authorizers.getAuthorizerProviderMap();
        if (providerMap.isEmpty()) {
            incomingPublishService.processPublish(ctx, msg, null);
            return;
        }

        final ClientAuthorizers clientAuthorizers = getClientAuthorizers(ctx);

        final AuthorizerProviderInput authorizerProviderInput =
                new AuthorizerProviderInputImpl(ctx.channel(), serverInformation, clientId);

        final PublishAuthorizerInputImpl input = new PublishAuthorizerInputImpl(msg, ctx.channel(), clientId);
        final PublishAuthorizerOutputImpl output = new PublishAuthorizerOutputImpl(asyncer);

        final SettableFuture<PublishAuthorizerOutputImpl> publishProcessedFuture =
                executePublishAuthorizer(clientId, providerMap, clientAuthorizers, authorizerProviderInput, input,
                        output, ctx);

        Futures.addCallback(
                publishProcessedFuture,
                new PublishAuthorizationProcessedTask(msg, ctx, mqttServerDisconnector,
                        incomingPublishService), MoreExecutors.directExecutor());
    }

    public void authorizeWillPublish(final @NotNull ChannelHandlerContext ctx, final @NotNull CONNECT connect) {

        final String clientId = ctx.channel().attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().getClientId();
        if (clientId == null || !ctx.channel().isActive()) {
            //no more processing needed, client is already disconnected
            return;
        }

        if (!authorizers.areAuthorizersAvailable() || connect.getWillPublish() == null) {
            ctx.pipeline().fireUserEventTriggered(new AuthorizeWillResultEvent(
                    connect,
                    new PublishAuthorizerResult(null, null, false)));
            return;
        }

        final Map<String, AuthorizerProvider> providerMap = authorizers.getAuthorizerProviderMap();
        if (providerMap.isEmpty()) {
            ctx.pipeline().fireUserEventTriggered(new AuthorizeWillResultEvent(
                    connect,
                    new PublishAuthorizerResult(null, null, false)));
            return;
        }

        final ClientAuthorizers clientAuthorizers = getClientAuthorizers(ctx);

        final AuthorizerProviderInput authorizerProviderInput =
                new AuthorizerProviderInputImpl(ctx.channel(), serverInformation, clientId);

        final PublishAuthorizerInputImpl input =
                new PublishAuthorizerInputImpl(connect.getWillPublish(), ctx.channel(), clientId);
        final PublishAuthorizerOutputImpl output = new PublishAuthorizerOutputImpl(asyncer);

        final SettableFuture<PublishAuthorizerOutputImpl> publishProcessedFuture =
                executePublishAuthorizer(clientId, providerMap, clientAuthorizers, authorizerProviderInput, input,
                        output, ctx);

        Futures.addCallback(
                publishProcessedFuture, new WillPublishAuthorizationProcessedTask(connect, ctx),
                MoreExecutors.directExecutor());
    }

    private @NotNull SettableFuture<PublishAuthorizerOutputImpl> executePublishAuthorizer(
            final @NotNull String clientId,
            final @NotNull Map<String, AuthorizerProvider> providerMap,
            final @NotNull ClientAuthorizers clientAuthorizers,
            final @NotNull AuthorizerProviderInput authorizerProviderInput,
            final @NotNull PublishAuthorizerInputImpl input,
            final @NotNull PublishAuthorizerOutputImpl output,
            final @NotNull ChannelHandlerContext ctx) {

        final SettableFuture<PublishAuthorizerOutputImpl> publishProcessedFuture = SettableFuture.create();

        final PublishAuthorizerContext context =
                new PublishAuthorizerContext(clientId, output, publishProcessedFuture, providerMap.size(), ctx);

        for (final Map.Entry<String, AuthorizerProvider> entry : providerMap.entrySet()) {
            final PublishAuthorizerTask task =
                    new PublishAuthorizerTask(entry.getValue(), entry.getKey(), authorizerProviderInput,
                            clientAuthorizers, ctx);
            pluginTaskExecutorService.handlePluginInOutTaskExecution(context, input, output, task);
        }
        return publishProcessedFuture;
    }

    public void authorizeSubscriptions(final @NotNull ChannelHandlerContext ctx, final @NotNull SUBSCRIBE msg) {

        final String clientId = ctx.channel().attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().getClientId();
        if (clientId == null || !ctx.channel().isActive()) {
            //no more processing needed
            return;
        }

        if (!authorizers.areAuthorizersAvailable()) {
            incomingSubscribeService.processSubscribe(ctx, msg, false);
            return;
        }

        final Map<String, AuthorizerProvider> providerMap = authorizers.getAuthorizerProviderMap();
        if (providerMap.isEmpty()) {
            incomingSubscribeService.processSubscribe(ctx, msg, false);
            return;
        }

        final ClientAuthorizers clientAuthorizers = getClientAuthorizers(ctx);

        final List<ListenableFuture<SubscriptionAuthorizerOutputImpl>> listenableFutures = new ArrayList<>();

        final AuthorizerProviderInput authorizerProviderInput =
                new AuthorizerProviderInputImpl(ctx.channel(), serverInformation, clientId);

        //every topic gets its own task per authorizer
        for (final Topic topic : msg.getTopics()) {

            final SubscriptionAuthorizerInputImpl input =
                    new SubscriptionAuthorizerInputImpl(UserPropertiesImpl.of(msg.getUserProperties().asList()), topic,
                            ctx.channel(), clientId);
            final SubscriptionAuthorizerOutputImpl output = new SubscriptionAuthorizerOutputImpl(asyncer);

            final SettableFuture<SubscriptionAuthorizerOutputImpl> topicProcessedFuture = SettableFuture.create();
            listenableFutures.add(topicProcessedFuture);

            final SubscriptionAuthorizerContext context =
                    new SubscriptionAuthorizerContext(clientId, output, topicProcessedFuture, providerMap.size());

            for (final Map.Entry<String, AuthorizerProvider> entry : providerMap.entrySet()) {
                final SubscriptionAuthorizerTask task =
                        new SubscriptionAuthorizerTask(entry.getValue(), entry.getKey(), authorizerProviderInput,
                                clientAuthorizers);
                pluginTaskExecutorService.handlePluginInOutTaskExecution(context, input, output, task);
            }
        }

        final AllTopicsProcessedTask allTopicsProcessedTask = new AllTopicsProcessedTask(msg, listenableFutures, ctx, mqttServerDisconnector, incomingSubscribeService);
        Futures.whenAllComplete(listenableFutures)
                .run(allTopicsProcessedTask, MoreExecutors.directExecutor());
    }

    private @NotNull ClientAuthorizers getClientAuthorizers(final @NotNull ChannelHandlerContext ctx) {
        final ClientConnection clientConnection = ctx.channel().attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();
        if (clientConnection.getExtensionClientAuthorizers() == null) {
            clientConnection.setExtensionClientAuthorizers(new ClientAuthorizersImpl(extensionPriorityComparator));
        }
        return clientConnection.getExtensionClientAuthorizers();
    }

    private void disconnectWithReasonCode(final @NotNull ChannelHandlerContext ctx, @NotNull final String logReason, final @NotNull String reasonString) {
        if (ctx.channel().isActive()) {
            final String logMessage = "Client (IP: {}) sent PUBLISH for " + logReason + ". This is not allowed. Disconnecting client.";
            final String reasonMessage = "Sent PUBLISH for " + reasonString;
            mqttServerDisconnector.disconnect(ctx.channel(),
                    logMessage,
                    reasonMessage,
                    Mqtt5DisconnectReasonCode.PROTOCOL_ERROR,
                    reasonMessage);
            ctx.close();
        }
    }

    public static class AuthorizeWillResultEvent {

        private final @NotNull CONNECT connect;

        private final @NotNull PublishAuthorizerResult result;

        public AuthorizeWillResultEvent(final @NotNull CONNECT connect, final @NotNull PublishAuthorizerResult result) {
            this.connect = connect;
            this.result = result;
        }

        @NotNull
        public CONNECT getConnect() {
            return connect;
        }

        @NotNull
        public PublishAuthorizerResult getResult() {
            return result;
        }
    }
}
