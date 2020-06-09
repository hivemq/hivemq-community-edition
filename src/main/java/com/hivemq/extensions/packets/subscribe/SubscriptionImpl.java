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
package com.hivemq.extensions.packets.subscribe;

import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.packets.subscribe.RetainHandling;
import com.hivemq.extension.sdk.api.packets.subscribe.Subscription;
import com.hivemq.mqtt.message.subscribe.Topic;

import java.util.Objects;

/**
 * @author Florian Limpöck
 * @author Silvio Giebl
 * @since 4.0.0
 */
@Immutable
public class SubscriptionImpl implements Subscription {

    final @NotNull String topicFilter;
    final @NotNull Qos qos;
    final @NotNull RetainHandling retainHandling;
    final boolean retainAsPublished;
    final boolean noLocal;

    public SubscriptionImpl(
            final @NotNull String topicFilter,
            final @NotNull Qos qos,
            final @NotNull RetainHandling retainHandling,
            final boolean retainAsPublished,
            final boolean noLocal) {

        this.topicFilter = topicFilter;
        this.qos = qos;
        this.retainHandling = retainHandling;
        this.retainAsPublished = retainAsPublished;
        this.noLocal = noLocal;
    }

    public SubscriptionImpl(final @NotNull Topic topic) {
        this(
                topic.getTopic(),
                topic.getQoS().toQos(),
                Objects.requireNonNull(RetainHandling.fromCode(topic.getRetainHandling().getCode())),
                topic.isRetainAsPublished(),
                topic.isNoLocal());
    }

    @Override
    public @NotNull String getTopicFilter() {
        return topicFilter;
    }

    @Override
    public @NotNull Qos getQos() {
        return qos;
    }

    @Override
    public @NotNull RetainHandling getRetainHandling() {
        return retainHandling;
    }

    @Override
    public boolean getRetainAsPublished() {
        return retainAsPublished;
    }

    @Override
    public boolean getNoLocal() {
        return noLocal;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SubscriptionImpl)) {
            return false;
        }
        final SubscriptionImpl that = (SubscriptionImpl) o;
        return topicFilter.equals(that.topicFilter) &&
                (qos == that.qos) &&
                (retainHandling == that.retainHandling) &&
                retainAsPublished == that.retainAsPublished &&
                noLocal == that.noLocal;
    }

    @Override
    public int hashCode() {
        return Objects.hash(topicFilter, qos, retainHandling, retainAsPublished, noLocal);
    }
}
