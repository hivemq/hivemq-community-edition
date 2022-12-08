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

package com.hivemq.extensions;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.hivemq.common.shutdown.HiveMQShutdownHook;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.embedded.EmbeddedExtension;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extensions.loader.ExtensionLifecycleHandler;
import com.hivemq.extensions.loader.ExtensionLoader;
import com.hivemq.extensions.services.auth.Authenticators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * @author Christoph Schäbel
 */
@Singleton
public class ExtensionBootstrapImpl implements ExtensionBootstrap {

    private static final Logger log = LoggerFactory.getLogger(ExtensionBootstrapImpl.class);

    private final @NotNull ExtensionLoader extensionLoader;
    private final @NotNull SystemInformation systemInformation;
    private final @NotNull ExtensionLifecycleHandler lifecycleHandler;
    private final @NotNull HiveMQExtensions hiveMQExtensions;
    private final @NotNull ShutdownHooks shutdownHooks;
    private final @NotNull Authenticators authenticators;

    @Inject
    public ExtensionBootstrapImpl(
            final @NotNull ExtensionLoader extensionLoader,
            final @NotNull SystemInformation systemInformation,
            final @NotNull ExtensionLifecycleHandler lifecycleHandler,
            final @NotNull HiveMQExtensions hiveMQExtensions,
            final @NotNull ShutdownHooks shutdownHooks,
            final @NotNull Authenticators authenticators) {
        this.extensionLoader = extensionLoader;
        this.systemInformation = systemInformation;
        this.lifecycleHandler = lifecycleHandler;
        this.hiveMQExtensions = hiveMQExtensions;
        this.shutdownHooks = shutdownHooks;
        this.authenticators = authenticators;
    }

    @NotNull
    @Override
    public CompletableFuture<Void> startExtensionSystem(final @Nullable EmbeddedExtension embeddedExtension) {
        log.info("Starting HiveMQ extension system.");

        shutdownHooks.add(new ExtensionSystemShutdownHook(this));
        final Path extensionFolder = systemInformation.getExtensionsFolder().toPath();

        // load already installed extensions
        final ImmutableCollection<HiveMQExtensionEvent> hiveMQExtensionEvents =
                extensionLoader.loadExtensions(extensionFolder, systemInformation.isEmbedded());

        final ImmutableList.Builder<HiveMQExtensionEvent> extensionEventBuilder =
                ImmutableList.<HiveMQExtensionEvent>builder().addAll(hiveMQExtensionEvents);

        if (embeddedExtension != null) {
            final HiveMQExtensionEvent extensionEvent = extensionLoader.loadEmbeddedExtension(embeddedExtension);
            if (extensionEvent != null) {
                extensionEventBuilder.add(extensionEvent);
            }
        }

        // start them if needed
        final ImmutableList<HiveMQExtensionEvent> allExtensions = extensionEventBuilder.build();
        return lifecycleHandler.handleExtensionEvents(allExtensions)
                .thenAccept(((v) -> authenticators.checkAuthenticationSafetyAndLifeness()));
    }

    @Override
    public void stopExtensionSystem() {
        final ImmutableList<HiveMQExtensionEvent> events = hiveMQExtensions.getEnabledHiveMQExtensions()
                .values()
                .stream()
                .map(extension -> new HiveMQExtensionEvent(
                        HiveMQExtensionEvent.Change.DISABLE,
                        extension.getId(),
                        extension.getStartPriority(),
                        extension.getExtensionFolderPath(),
                        extension.isEmbedded()))
                .collect(ImmutableList.toImmutableList());

        // stop extensions
        lifecycleHandler.handleExtensionEvents(events).join();
        // not checking for authenticator safety
    }

    private static class ExtensionSystemShutdownHook implements HiveMQShutdownHook {

        private static final Logger log = LoggerFactory.getLogger(ExtensionSystemShutdownHook.class);

        private final @NotNull ExtensionBootstrap extensionBootstrap;

        private ExtensionSystemShutdownHook(final @NotNull ExtensionBootstrap extensionBootstrap) {
            this.extensionBootstrap = extensionBootstrap;
        }

        @Override
        public @NotNull String name() {
            return "Extension System Shutdown Hook";
        }

        @Override
        public @NotNull Priority priority() {
            return Priority.VERY_LOW;
        }

        @Override
        public void run() {
            log.info("Shutting down extension system");
            try {
                extensionBootstrap.stopExtensionSystem();
            } catch (final Exception e) {
                log.error("Exception at Extension system shutdown", e);
            }
        }
    }
}
