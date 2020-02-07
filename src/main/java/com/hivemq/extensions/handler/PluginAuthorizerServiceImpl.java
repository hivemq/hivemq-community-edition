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

package com.hivemq.extensions.handler;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.parameter.AuthorizerProviderInput;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extension.sdk.api.services.auth.provider.AuthorizerProvider;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.PluginPriorityComparator;
import com.hivemq.extensions.auth.parameter.*;
import com.hivemq.extensions.client.ClientAuthorizers;
import com.hivemq.extensions.client.ClientAuthorizersImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.handler.tasks.*;
import com.hivemq.extensions.services.auth.Authorizers;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.handler.disconnect.Mqtt3ServerDisconnector;
import com.hivemq.mqtt.handler.disconnect.Mqtt5ServerDisconnector;
import com.hivemq.mqtt.handler.publish.IncomingPublishService;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.subscribe.SUBSCRIBE;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.ChannelUtils;
import com.hivemq.util.Topics;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(PluginAuthorizerService.class);

    private final @NotNull Authorizers authorizers;
    private final @NotNull PluginOutPutAsyncer asyncer;
    private final @NotNull PluginTaskExecutorService pluginTaskExecutorService;
    private final @NotNull ServerInformation serverInformation;
    private final @NotNull Mqtt3ServerDisconnector mqtt3Disconnector;
    private final @NotNull Mqtt5ServerDisconnector mqtt5Disconnector;
    private final @NotNull EventLog eventLog;
    private final @NotNull PluginPriorityComparator pluginPriorityComparator;
    private final @NotNull IncomingPublishService incomingPublishService;

    private final boolean allowDollarTopics;

    @Inject
    public PluginAuthorizerServiceImpl(
            final @NotNull Authorizers authorizers,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull PluginTaskExecutorService pluginTaskExecutorService,
            final @NotNull ServerInformation serverInformation,
            final @NotNull HiveMQExtensions hiveMQExtensions,
            final @NotNull Mqtt3ServerDisconnector mqtt3Disconnector,
            final @NotNull Mqtt5ServerDisconnector mqtt5Disconnector,
            final @NotNull EventLog eventLog,
            final @NotNull IncomingPublishService incomingPublishService) {

        this.authorizers = authorizers;
        this.asyncer = asyncer;
        this.pluginTaskExecutorService = pluginTaskExecutorService;
        this.serverInformation = serverInformation;
        this.mqtt3Disconnector = mqtt3Disconnector;
        this.mqtt5Disconnector = mqtt5Disconnector;
        this.eventLog = eventLog;
        this.incomingPublishService = incomingPublishService;
        this.pluginPriorityComparator = new PluginPriorityComparator(hiveMQExtensions);
        this.allowDollarTopics = MQTT_ALLOW_DOLLAR_TOPICS.get();
    }

    public void authorizePublish(final @NotNull ChannelHandlerContext ctx, final @NotNull PUBLISH msg) {

        //We first check if the topic is allowed to be published
        if (!Topics.isValidTopicToPublish(msg.getTopic())) {
            if (log.isDebugEnabled()) {
                final String clientId = ChannelUtils.getClientId(ctx.channel());
                log.debug(
                        "Client '" + clientId +
                                "' (IP: {}) published to an invalid topic ( '{}' ). Disconnecting client.",
                        ChannelUtils.getChannelIP(ctx.channel()).or("UNKNOWN"), msg.getTopic());
            }
            disconnectByClose(ctx, "Sent PUBLISH for an invalid topic");
            return;
        }

        // if $ topics are not allowed
        if (!allowDollarTopics && Topics.isDollarTopic(msg.getTopic())) {
            if (log.isDebugEnabled()) {
                final String clientId = ChannelUtils.getClientId(ctx.channel());
                log.debug(
                        "Client '" + clientId +
                                "' (IP: {}) published to a topic starting with '$' ( '{}' ). Disconnecting client.",
                        ChannelUtils.getChannelIP(ctx.channel()).or("UNKNOWN"), msg.getTopic());
            }
            disconnectByClose(ctx, "Sent PUBLISH for an topic that starts with '$'");
            return;
        }


        final String clientId = ctx.channel().attr(ChannelAttributes.CLIENT_ID).get();

        final Runnable defaultProcessTask = () -> incomingPublishService.processPublish(ctx, msg, null);
        if (clientId == null) {
            //we must process the msg in every case !
            ctx.executor().execute(defaultProcessTask);
            return;
        }

        if (!authorizers.areAuthorizersAvailable()) {
            ctx.executor().execute(defaultProcessTask);
            return;
        }

        final Map<String, AuthorizerProvider> providerMap = authorizers.getAuthorizerProviderMap();
        if (providerMap.isEmpty()) {
            ctx.executor().execute(defaultProcessTask);
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
                new PublishAuthorizationProcessedTask(msg, ctx, mqtt5Disconnector, mqtt3Disconnector,
                        incomingPublishService), MoreExecutors.directExecutor());
    }

    public void authorizeWillPublish(final @NotNull ChannelHandlerContext ctx, final @NotNull CONNECT connect) {

        final String clientId = ctx.channel().attr(ChannelAttributes.CLIENT_ID).get();
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

        final String clientId = ctx.channel().attr(ChannelAttributes.CLIENT_ID).get();
        if (clientId == null || !ctx.channel().isActive()) {
            //no more processing needed
            return;
        }

        if (!authorizers.areAuthorizersAvailable()) {
            ctx.fireChannelRead(msg);
            return;
        }

        final Map<String, AuthorizerProvider> providerMap = authorizers.getAuthorizerProviderMap();
        if (providerMap.isEmpty()) {
            ctx.fireChannelRead(msg);
            return;
        }

        final ClientAuthorizers clientAuthorizers = getClientAuthorizers(ctx);

        final List<ListenableFuture<SubscriptionAuthorizerOutputImpl>> listenableFutures = new ArrayList<>();

        final AuthorizerProviderInput authorizerProviderInput =
                new AuthorizerProviderInputImpl(ctx.channel(), serverInformation, clientId);

        //every topic gets its own task per authorizer
        for (final Topic topic : msg.getTopics()) {

            final SubscriptionAuthorizerInputImpl input =
                    new SubscriptionAuthorizerInputImpl(msg.getUserProperties().getPluginUserProperties(), topic,
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

        Futures.whenAllComplete(listenableFutures)
                .run(
                        new AllTopicsProcessedTask(msg, listenableFutures, ctx, mqtt5Disconnector, mqtt3Disconnector),
                        MoreExecutors.directExecutor());
    }

    @NotNull
    private ClientAuthorizers getClientAuthorizers(final @NotNull ChannelHandlerContext ctx) {
        ClientAuthorizers clientAuthorizers = ctx.channel().attr(ChannelAttributes.PLUGIN_CLIENT_AUTHORIZERS).get();
        if (clientAuthorizers == null) {

            clientAuthorizers = new ClientAuthorizersImpl(pluginPriorityComparator);
            ctx.channel().attr(ChannelAttributes.PLUGIN_CLIENT_AUTHORIZERS).set(clientAuthorizers);
        }
        return clientAuthorizers;
    }

    private void disconnectByClose(final ChannelHandlerContext ctx, final @NotNull String reason) {
        if (ctx.channel().isActive()) {
            eventLog.clientWasDisconnected(ctx.channel(), reason);
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
