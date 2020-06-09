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
package com.hivemq.limitation;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.configuration.service.InternalConfigurations;

import javax.inject.Singleton;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
@Singleton
public class TopicAliasLimiterImpl implements TopicAliasLimiter {

    private final @NotNull AtomicLong memoryUsage;
    private final @NotNull AtomicLong topicAliasesTotal;

    private final int memorySoftLimit;
    private final int memoryHardLimit;

    public TopicAliasLimiterImpl() {
        this.memoryUsage = new AtomicLong(0);
        this.topicAliasesTotal = new AtomicLong(0);
        this.memorySoftLimit = InternalConfigurations.TOPIC_ALIAS_GLOBAL_MEMORY_SOFT_LIMIT.get();
        this.memoryHardLimit = InternalConfigurations.TOPIC_ALIAS_GLOBAL_MEMORY_HARD_LIMIT.get();
    }

    @Override
    public boolean aliasesAvailable() {
        return memoryUsage.get() < memorySoftLimit;
    }

    @Override
    public boolean limitExceeded() {
        return this.memoryUsage.get() > memoryHardLimit;
    }

    @Override
    public void initUsage(final int size) {
        //4 bytes per topic as index
        this.memoryUsage.addAndGet(size * 4);
    }

    @Override
    public void addUsage(@NotNull final String topic) {
        this.memoryUsage.addAndGet(getEstimatedSize(topic));
        this.topicAliasesTotal.incrementAndGet();
    }

    @Override
    public void removeUsage(final String... topics) {
        for (final String topic : topics) {
            if (topic != null) {
                this.memoryUsage.addAndGet(-1 * getEstimatedSize(topic));
                this.topicAliasesTotal.decrementAndGet();
            }
        }
    }

    @Override
    public void finishUsage(@NotNull final String... topics) {
        //4 bytes per topic as index
        this.memoryUsage.addAndGet(topics.length * -4);
        this.removeUsage(topics);
    }

    /**
     * 38 = estimated String overhead
     * 2  = per character of a topic
     *
     * @param topic to estimate size
     * @return the size in memory of a topic
     */
    private int getEstimatedSize(final @NotNull String topic) {
        return 38 + (topic.length() * 2);
    }

}
