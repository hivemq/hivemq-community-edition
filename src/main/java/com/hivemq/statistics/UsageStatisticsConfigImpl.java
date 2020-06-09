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
package com.hivemq.statistics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Christoph Schäbel
 */
public class UsageStatisticsConfigImpl implements UsageStatisticsConfig {

    private static final Logger log = LoggerFactory.getLogger(UsageStatisticsConfigImpl.class);

    private final AtomicBoolean enabled = new AtomicBoolean(false);

    @Override
    public boolean isEnabled() {
        return this.enabled.get();
    }

    @Override
    public void setEnabled(final boolean enabled) {
        log.debug("Setting anonymous usage statistics enabled to {} ", enabled);
        this.enabled.set(enabled);
    }
}
