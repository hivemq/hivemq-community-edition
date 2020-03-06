/*
 * Copyright 2019 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hivemq.extensions;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.common.shutdown.HiveMQShutdownHook;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extensions.loader.PluginLifecycleHandler;
import com.hivemq.extensions.loader.PluginLoader;
import com.hivemq.extensions.services.auth.Authenticators;
import com.hivemq.persistence.util.FutureUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.ExecutionException;

/**
 * @author Christoph Schäbel
 */
@Singleton
public class PluginBootstrapImpl implements PluginBootstrap {

    private static final Logger log = LoggerFactory.getLogger(PluginBootstrapImpl.class);

    @NotNull
    private final PluginLoader pluginLoader;
    @NotNull
    private final SystemInformation systemInformation;
    @NotNull
    private final PluginLifecycleHandler lifecycleHandler;
    @NotNull
    private final HiveMQExtensions hiveMQExtensions;
    @NotNull
    private final ShutdownHooks shutdownHooks;
    @NotNull
    private final Authenticators authenticators;

    @Inject
    public PluginBootstrapImpl(@NotNull final PluginLoader pluginLoader,
                               @NotNull final SystemInformation systemInformation,
                               @NotNull final PluginLifecycleHandler lifecycleHandler,
                               @NotNull final HiveMQExtensions hiveMQExtensions,
                               @NotNull final ShutdownHooks shutdownHooks,
                               @NotNull final Authenticators authenticators) {
        this.pluginLoader = pluginLoader;
        this.systemInformation = systemInformation;
        this.lifecycleHandler = lifecycleHandler;
        this.hiveMQExtensions = hiveMQExtensions;
        this.shutdownHooks = shutdownHooks;
        this.authenticators = authenticators;
    }

    @Override
    public void startPluginSystem() {

        log.info("Starting HiveMQ extension system.");

        shutdownHooks.add(new PluginSystemShutdownHook(this));

        //load already installed extensions
        final ImmutableList<HiveMQPluginEvent> hiveMQPluginEvents = pluginLoader.loadPlugins(systemInformation.getExtensionsFolder().toPath(), ExtensionMain.class);

        //start them if needed
        lifecycleHandler.handlePluginEvents(hiveMQPluginEvents)
                .thenAccept(((v) -> authenticators.checkAuthenticationSafetyAndLifeness()));
    }

    @NotNull
    @Override
    public void stopPluginSystem() {

        final ImmutableList<HiveMQPluginEvent> events = hiveMQExtensions.getEnabledHiveMQExtensions()
                .values().stream()
                .map(extension -> new HiveMQPluginEvent(HiveMQPluginEvent.Change.DISABLE, extension.getId(), extension.getStartPriority(), extension.getPluginFolderPath()))
                .collect(ImmutableList.toImmutableList());

        //stop extensions
        lifecycleHandler.handlePluginEvents(events).join();
        // not checking for authenticator safety
    }

    private static class PluginSystemShutdownHook extends HiveMQShutdownHook {

        private static final Logger log = LoggerFactory.getLogger(PluginSystemShutdownHook.class);

        @NotNull
        private final PluginBootstrap pluginBootstrap;

        private PluginSystemShutdownHook(@NotNull final PluginBootstrap pluginBootstrap) {
            this.pluginBootstrap = pluginBootstrap;
        }

        @NotNull
        @Override
        public String name() {
            return "Extension System Shutdown Hook";
        }

        @NotNull
        @Override
        public Priority priority() {
            return Priority.VERY_LOW;
        }

        @Override
        public boolean isAsynchronous() {
            return false;
        }

        @Override
        public void run() {
            log.info("Shutting down extension system");

            try {
                pluginBootstrap.stopPluginSystem();
            } catch (final Exception e) {
                log.error("Exception at Extension system shutdown", e);
            }
        }
    }
}
