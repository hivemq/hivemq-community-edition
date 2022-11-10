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
package com.hivemq.persistence.clientsession;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.*;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.handler.publish.PublishReturnCode;
import com.hivemq.mqtt.message.connect.Mqtt5CONNECT;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.services.InternalPublishService;
import com.hivemq.persistence.ioc.annotation.Persistence;
import com.hivemq.persistence.local.ClientSessionLocalPersistence;
import com.hivemq.util.Checkpoints;
import com.hivemq.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hivemq.configuration.service.InternalConfigurations.WILL_DELAY_CHECK_INTERVAL_SEC;

/**
 * @author Lukas Brandl
 */
@Singleton
public class PendingWillMessages {

    private static final Logger log = LoggerFactory.getLogger(PendingWillMessages.class);

    private final @NotNull Map<String, PendingWill> pendingWills = new ConcurrentHashMap<>();
    private final @NotNull InternalPublishService publishService;
    private final @NotNull ListeningScheduledExecutorService executorService;
    private final @NotNull ClientSessionPersistence clientSessionPersistence;
    private final @NotNull ClientSessionLocalPersistence clientSessionLocalPersistence;
    private final @NotNull MetricsHolder metricsHolder;

    @Inject
    public PendingWillMessages(
            final @NotNull InternalPublishService publishService,
            final @Persistence @NotNull ListeningScheduledExecutorService executorService,
            final @NotNull ClientSessionPersistence clientSessionPersistence,
            final @NotNull ClientSessionLocalPersistence clientSessionLocalPersistence,
            final @NotNull MetricsHolder metricsHolder) {

        this.publishService = publishService;
        this.executorService = executorService;
        this.clientSessionPersistence = clientSessionPersistence;
        this.clientSessionLocalPersistence = clientSessionLocalPersistence;
        this.metricsHolder = metricsHolder;
        executorService.scheduleAtFixedRate(new CheckWillsTask(), WILL_DELAY_CHECK_INTERVAL_SEC, WILL_DELAY_CHECK_INTERVAL_SEC, TimeUnit.SECONDS);
    }

    public void sendOrEnqueueWillIfAvailable(final @NotNull String clientId, final @NotNull ClientSession session) {
        checkNotNull(clientId, "Client id must not be null");
        checkNotNull(session, "Client session must not be null");
        final ClientSessionWill sessionWill = session.getWillPublish();
        if (session.getWillPublish() == null) {
            return;
        }
        if (sessionWill.getDelayInterval() == 0 || session.getSessionExpiryInterval() == Mqtt5CONNECT.SESSION_EXPIRE_ON_DISCONNECT) {
            sendWill(clientId, publishFromWill(sessionWill));
            return;
        }
        pendingWills.put(clientId, new PendingWill(Math.min(sessionWill.getDelayInterval(), session.getSessionExpiryInterval()), System.currentTimeMillis()));
    }

    public void sendWillIfPending(final @NotNull String clientId) {
        checkNotNull(clientId, "Client id must not be null");
        final PendingWill pendingWill = pendingWills.remove(clientId);
        if (pendingWill != null) {
            getAndSendPendingWill(clientId);
        }
    }

    private void getAndSendPendingWill(final @NotNull String clientId) {
        // We expect that the session and its will still exist if we found a pending will to send.
        // Assert this to fail early.
        // Because assertions are removed in production, additionally log a warning because if the session is gone,
        // we can't reason about whether we ever send the will or not before, here or via another component.
        // This may or may not indicate a previously unknown race condition.
        final ClientSession session = clientSessionLocalPersistence.getSession(clientId, false);
        assert session != null : "Missing expected session to get will message from for client: " + clientId;
        //noinspection ConstantConditions
        if (session == null) {
            log.warn("Unable to send pending will for client {} because the session is gone.", clientId);
            return;
        }
        final ClientSessionWill sessionWill = session.getWillPublish();
        assert sessionWill != null : "Missing expected will message in session of client: " + clientId;
        //noinspection ConstantConditions
        if (sessionWill == null) {
            log.warn("Unable to send pending will for client {} because the session's will is gone.", clientId);
            return;
        }
        sendWill(clientId, publishFromWill(sessionWill));
    }

    public void cancelWillIfPending(final @NotNull String clientId) {
        pendingWills.remove(clientId);
    }

    public void reset() {
        pendingWills.clear();
        final ListenableFuture<Map<String, PendingWill>> future = clientSessionPersistence.pendingWills();
        Futures.addCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(final @NotNull Map<String, PendingWill> result) {
                pendingWills.putAll(result);
            }

            @Override
            public void onFailure(final @NotNull Throwable t) {
                Exceptions.rethrowError("Exception when reading pending will messages", t);
            }
        }, MoreExecutors.directExecutor());
    }

    private void sendWill(final @NotNull String clientId, final @NotNull PUBLISH willPublish) {
        Futures.addCallback(publishService.publish(willPublish, executorService, clientId), new FutureCallback<>() {
            @Override
            public void onSuccess(final @NotNull PublishReturnCode result) {
                metricsHolder.getPublishedWillMessagesCount().inc();
            }

            @Override
            public void onFailure(final @NotNull Throwable t) {
                log.error("Publish of Will message failed.", t);
                Exceptions.rethrowError(t);
            }
        }, MoreExecutors.directExecutor());


        final ListenableFuture<Void> future = clientSessionPersistence.deleteWill(clientId);
        if (Checkpoints.enabled()) {
            future.addListener(() -> Checkpoints.checkpoint("pending-will-removed"), MoreExecutors.directExecutor());
        }
    }

    private static @NotNull PUBLISH publishFromWill(final @NotNull ClientSessionWill sessionWill) {
        return new PUBLISHFactory.Mqtt5Builder().withTopic(sessionWill.getTopic()).withQoS(sessionWill.getQos()).withOnwardQos(sessionWill.getQos()).withPayload(sessionWill.getPayload())
                .withRetain(sessionWill.isRetain()).withHivemqId(sessionWill.getHivemqId()).withUserProperties(sessionWill.getUserProperties())
                .withResponseTopic(sessionWill.getResponseTopic()).withCorrelationData(sessionWill.getCorrelationData())
                .withContentType(sessionWill.getContentType()).withPayloadFormatIndicator(sessionWill.getPayloadFormatIndicator())
                .withMessageExpiryInterval(sessionWill.getMessageExpiryInterval()).build();
    }

    @VisibleForTesting
    public @NotNull Map<String, PendingWill> getPendingWills() {
        return pendingWills;
    }

    class CheckWillsTask implements Runnable {
        @Override
        public void run() {
            try {
                for (final String clientId : pendingWills.keySet()) {
                    // To avoid a race that could lead to sending duplicates, we must use a compute method to atomically
                    // treat a PendingWill because there could be concurrent calls to sendWillIfPending which remove and
                    // treat an entry.
                    pendingWills.computeIfPresent(clientId, (clientIdKey, pendingWill) -> {
                        if (pendingWill.getStartTime() + pendingWill.getDelayInterval() * 1000 < System.currentTimeMillis()) {
                            getAndSendPendingWill(clientIdKey);
                            return null;
                        }
                        return pendingWill;
                    });
                }
            } catch (final Exception e) {
                log.error("Exception while checking pending will messages", e);
            }
        }
    }

    public static class PendingWill {
        private final long delayInterval;
        private final long startTime;

        public PendingWill(final long delayInterval, final long startTime) {
            this.delayInterval = delayInterval;
            this.startTime = startTime;
        }

        public long getDelayInterval() {
            return delayInterval;
        }

        public long getStartTime() {
            return startTime;
        }
    }
}
