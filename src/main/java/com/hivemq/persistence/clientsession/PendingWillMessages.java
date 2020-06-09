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
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.services.InternalPublishService;
import com.hivemq.persistence.ioc.annotation.Persistence;
import com.hivemq.persistence.local.ClientSessionLocalPersistence;
import com.hivemq.persistence.util.FutureUtils;
import com.hivemq.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hivemq.configuration.service.InternalConfigurations.WILL_DELAY_CHECK_SCHEDULE;

/**
 * @author Lukas Brandl
 */
@Singleton
public class PendingWillMessages {

    private static final Logger log = LoggerFactory.getLogger(PendingWillMessages.class);

    @NotNull
    private final InternalPublishService publishService;

    @NotNull
    private final ListeningScheduledExecutorService executorService;

    @VisibleForTesting
    final Map<String, PendingWill> pendingWills = new ConcurrentHashMap<>();
    @NotNull
    private final ClientSessionPersistence clientSessionPersistence;

    @NotNull
    private final ClientSessionLocalPersistence clientSessionLocalPersistence;

    @Inject
    public PendingWillMessages(@NotNull final InternalPublishService publishService,
                               @Persistence final ListeningScheduledExecutorService executorService,
                               @NotNull final ClientSessionPersistence clientSessionPersistence,
                               @NotNull final ClientSessionLocalPersistence clientSessionLocalPersistence) {
        this.publishService = publishService;
        this.executorService = executorService;
        this.clientSessionPersistence = clientSessionPersistence;
        this.clientSessionLocalPersistence = clientSessionLocalPersistence;
        executorService.scheduleAtFixedRate(new CheckWillsTask(), WILL_DELAY_CHECK_SCHEDULE, WILL_DELAY_CHECK_SCHEDULE, TimeUnit.SECONDS);

    }

    public void addWill(@NotNull final String clientId, @NotNull final ClientSession session) {
        checkNotNull(clientId, "Client id must not be null");
        checkNotNull(session, "Client session must not be null");
        final ClientSessionWill willPublish = session.getWillPublish();
        if (session.getWillPublish() == null) {
            return;
        }
        if (willPublish.getDelayInterval() == 0 || session.getSessionExpiryInterval() == CONNECT.SESSION_EXPIRE_ON_DISCONNECT) {
            sendWill(clientId, session);
            return;
        }
        pendingWills.put(clientId, new PendingWill(Math.min(willPublish.getDelayInterval(), session.getSessionExpiryInterval()), System.currentTimeMillis()));
    }

    public void cancelWill(@NotNull final String clientId) {
        pendingWills.remove(clientId);
    }

    public void reset() {
        pendingWills.clear();
        final ListenableFuture<Map<String, PendingWill>> future = clientSessionPersistence.pendingWills();
        FutureUtils.addPersistenceCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(@NotNull final Map<String, PendingWill> result) {
                pendingWills.putAll(result);
            }

            @Override
            public void onFailure(@NotNull final Throwable t) {
                Exceptions.rethrowError("Exception when reading pending will messages", t);
            }
        });
    }

    private void sendWill(@NotNull final String clientId, @Nullable final ClientSession session) {
        if (session != null && session.getWillPublish() != null) {
            final PUBLISH publish = publishFromWill(session.getWillPublish());
            publishService.publish(publish, executorService, clientId);
            clientSessionPersistence.removeWill(clientId);
        }
    }

    private PUBLISH publishFromWill(final ClientSessionWill sessionWill) {
        return new PUBLISHFactory.Mqtt5Builder().withTopic(sessionWill.getTopic()).withQoS(sessionWill.getQos()).withPayload(sessionWill.getPayload())
                .withRetain(sessionWill.isRetain()).withHivemqId(sessionWill.getHivemqId()).withUserProperties(sessionWill.getUserProperties())
                .withResponseTopic(sessionWill.getResponseTopic()).withCorrelationData(sessionWill.getCorrelationData())
                .withContentType(sessionWill.getContentType()).withPayloadFormatIndicator(sessionWill.getPayloadFormatIndicator())
                .withMessageExpiryInterval(sessionWill.getMessageExpiryInterval()).build();
    }

    class CheckWillsTask implements Runnable {
        @Override
        public void run() {
            try {
                final Iterator<Map.Entry<String, PendingWill>> iterator = pendingWills.entrySet().iterator();
                while (iterator.hasNext()) {
                    final Map.Entry<String, PendingWill> entry = iterator.next();
                    final String clientId = entry.getKey();
                    final PendingWill pendingWill = entry.getValue();
                    if (pendingWill.getStartTime() + pendingWill.getDelayInterval() * 1000 < System.currentTimeMillis()) {
                        sendWill(clientId, clientSessionLocalPersistence.getSession(clientId, false));
                        iterator.remove();
                    }
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
