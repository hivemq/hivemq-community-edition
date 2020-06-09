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
package com.hivemq.metrics.gauges;

import com.codahale.metrics.Gauge;
import com.hivemq.persistence.retained.RetainedMessagePersistence;

/**
 * @author Christoph Schäbel
 */
public class RetainedMessagesGauge implements Gauge<Long> {

    private final RetainedMessagePersistence retainedMessagePersistence;

    public RetainedMessagesGauge(final RetainedMessagePersistence retainedMessagePersistence) {
        this.retainedMessagePersistence = retainedMessagePersistence;
    }

    @Override
    public Long getValue() {
        try {
            return retainedMessagePersistence.size();
        } catch (final Exception ignore) {
        }
        return 0L;
    }
}
