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
package com.hivemq.metrics;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.common.shutdown.HiveMQShutdownHook;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.metrics.jmx.JmxReporterBootstrap;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Lukas Brandl
 */
@Singleton
public class MetricsShutdownHook extends HiveMQShutdownHook {

    private final ShutdownHooks shutdownHooks;
    private final JmxReporterBootstrap jmxReporterBootstrap;

    @Inject
    public MetricsShutdownHook(final ShutdownHooks shutdownHooks, final JmxReporterBootstrap jmxReporterBootstrap) {
        this.shutdownHooks = shutdownHooks;
        this.jmxReporterBootstrap = jmxReporterBootstrap;
    }

    @PostConstruct
    public void postConstruct() {
        shutdownHooks.add(this);
    }

    @Override
    public void run() {
        jmxReporterBootstrap.stop();
    }

    @NotNull
    @Override
    public String name() {
        return "Metrics Shutdown";
    }

    @NotNull
    @Override
    public Priority priority() {
        return Priority.DOES_NOT_MATTER;
    }

    @Override
    public boolean isAsynchronous() {
        return false;
    }
}
