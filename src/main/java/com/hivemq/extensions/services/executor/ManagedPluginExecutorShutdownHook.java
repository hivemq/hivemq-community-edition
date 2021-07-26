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
package com.hivemq.extensions.services.executor;

import com.hivemq.common.shutdown.HiveMQShutdownHook;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class ManagedPluginExecutorShutdownHook implements HiveMQShutdownHook {

    private static final Logger log = LoggerFactory.getLogger(ManagedPluginExecutorShutdownHook.class);

    private final @NotNull GlobalManagedExtensionExecutorService scheduledExecutorService;
    private final int timeout;

    public ManagedPluginExecutorShutdownHook(
            final @NotNull GlobalManagedExtensionExecutorService scheduledExecutorService, final int timeout) {
        this.scheduledExecutorService = scheduledExecutorService;
        this.timeout = timeout;
    }

    @Override
    public @NotNull String name() {
        return "ManagedExtensionExecutorService shutdown";
    }

    @Override
    public @NotNull Priority priority() {
        //must be lower than extension shut down hook which is VERY_LOW
        return Priority.DOES_NOT_MATTER;
    }

    @Override
    public void run() {

        log.debug("Shutting down managed extension executor service");
        scheduledExecutorService.shutdown();
        try {
            if (!scheduledExecutorService.awaitTermination(timeout, TimeUnit.SECONDS)) {
                scheduledExecutorService.shutdownNow();
                log.warn(
                        "Termination of managed extension executor service timed out after {} seconds. Enforcing shutdown.",
                        timeout);
            }
        } catch (final InterruptedException ex) {
            scheduledExecutorService.shutdownNow();
            log.warn("Not able to wait for managed extension executor service shutdown. Enforcing shutdown.", ex);
        }
    }
}
