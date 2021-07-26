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
package com.hivemq.security.ioc;

import com.hivemq.common.shutdown.HiveMQShutdownHook;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.util.ThreadFactoryUtil;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author Georg Held
 */
@Singleton
public class SecurityExecutorProvider implements Provider<ScheduledExecutorService> {

    private final @NotNull ScheduledExecutorService sslContextStoreService;

    @Inject
    SecurityExecutorProvider(final @NotNull ShutdownHooks shutdownHooks) {
        sslContextStoreService =
                Executors.newScheduledThreadPool(2, ThreadFactoryUtil.create("ssl-context-executor-%d"));
        shutdownHooks.add(new HiveMQShutdownHook() {
            @Override
            public @NotNull String name() {
                return "Ssl Context Store Executor Shutdown";
            }

            @Override
            public @NotNull Priority priority() {
                return Priority.MEDIUM;
            }

            @Override
            public void run() {
                sslContextStoreService.shutdownNow();
            }
        });

    }

    @Override
    public ScheduledExecutorService get() {
        return sslContextStoreService;
    }
}
