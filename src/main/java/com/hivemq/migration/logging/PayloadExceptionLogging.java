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
package com.hivemq.migration.logging;

import com.google.common.annotations.VisibleForTesting;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.migration.Migrations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Formatter;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Georg Held
 */
@LazySingleton
public class PayloadExceptionLogging {

    private static final Logger migrationLog = LoggerFactory.getLogger(Migrations.MIGRATION_LOGGER_NAME);
    private static final int LOGGING_INTERVAL = 10_000;
    private static final String bigLine = "================================================================================";
    private static final String smallLine = "--------------------------------------------------------------------------------";
    private int counter = 0;

    @NotNull
    private final Map<Long, MissingMessageInformation> payloadIdMissingMessagesMap;

    @VisibleForTesting
    public PayloadExceptionLogging() {
        payloadIdMissingMessagesMap = new TreeMap<>();
    }

    public synchronized void addLogging(final long payloadId,
                                        @Nullable final Boolean retained,
                                        @Nullable final String retainedTopic) {

        final MissingMessageInformation information;
        if (payloadIdMissingMessagesMap.containsKey(payloadId)) {
            information = payloadIdMissingMessagesMap.get(payloadId);
        } else {
            information = new MissingMessageInformation(payloadId);
        }

        if (retained != null && retainedTopic != null) {
            information.setRetained(retained);
            information.setTopic(retainedTopic);
        }

        payloadIdMissingMessagesMap.put(payloadId, information);

        counter++;
        if (counter > LOGGING_INTERVAL) {
            logAndClear();
        }
    }


    public synchronized void logAndClear() {

        if (payloadIdMissingMessagesMap.isEmpty()) {
            return;
        }

        final StringBuilder stringBuilder = new StringBuilder();
        final Formatter formatter = new Formatter(stringBuilder);

        formatter.format("%n%1$31s%n%n", "MISSING PAYLOADS");
        formatter.format("%1$19s | %2$8s | %3$47s %n", "payloadId", "retained", "topic");
        formatter.format("%1$s%n", bigLine);
        for (final Map.Entry<Long, MissingMessageInformation> entry : payloadIdMissingMessagesMap.entrySet()) {
            final MissingMessageInformation missingMessage = entry.getValue();
            formatter.format("%1$19d | %2$8b | %3$47s %n", missingMessage.getPayloadId(), missingMessage.isRetained(), missingMessage.getTopic());
            formatter.format("%n%1$s%n", smallLine);
        }

        formatter.flush();
        migrationLog.warn(stringBuilder.toString());
        payloadIdMissingMessagesMap.clear();
    }

    @NotNull
    @VisibleForTesting
    Map<Long, MissingMessageInformation> getMap() {
        return payloadIdMissingMessagesMap;
    }

    static class MissingMessageInformation {
        private final long payloadId;
        private boolean retained;
        private @Nullable String topic;

        private MissingMessageInformation(final long payloadId) {
            this.payloadId = payloadId;
        }

        public long getPayloadId() {
            return payloadId;
        }

        public boolean isRetained() {
            return retained;
        }

        public void setRetained(final boolean retained) {
            this.retained = retained;
        }

        @Nullable
        public String getTopic() {
            return topic;
        }

        public void setTopic(final @NotNull String topic) {
            this.topic = topic;
        }
    }
}
