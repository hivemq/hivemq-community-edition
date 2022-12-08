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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.hivemq.common.annotations.GuardedBy;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.annotations.ThreadSafe;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extensions.parameter.ExtensionStartOutputImpl;
import com.hivemq.extensions.parameter.ExtensionStartStopInputImpl;
import com.hivemq.extensions.parameter.ExtensionStopOutputImpl;
import com.hivemq.util.Checkpoints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Georg Held
 * @author Silvio Giebl
 */
@ThreadSafe
@Singleton
public class HiveMQExtensions {

    private static final Logger log = LoggerFactory.getLogger(HiveMQExtensions.class);

    @GuardedBy("extensionsLock")
    private final @NotNull HashMap<String, HiveMQExtension> knownExtensions = new HashMap<>();
    @GuardedBy("classloaderLock")
    private final @NotNull HashMap<ClassLoader, HiveMQExtension> classloaderToExtension = new HashMap<>();
    @GuardedBy("beforeExtensionStopCallbacksLock")
    private final @NotNull List<Consumer<HiveMQExtension>> beforeExtensionStopCallbacks = new LinkedList<>();
    @GuardedBy("afterExtensionStopCallbacksLock")
    private final @NotNull List<Consumer<HiveMQExtension>> afterExtensionStopCallbacks = new LinkedList<>();

    private final @NotNull ReadWriteLock extensionsLock = new ReentrantReadWriteLock();
    private final @NotNull ReadWriteLock classloaderLock = new ReentrantReadWriteLock();
    private final @NotNull ReadWriteLock beforeExtensionStopCallbacksLock = new ReentrantReadWriteLock();
    private final @NotNull ReadWriteLock afterExtensionStopCallbacksLock = new ReentrantReadWriteLock();
    private final @NotNull ServerInformation serverInformation;

    @Inject
    public HiveMQExtensions(final @NotNull ServerInformation serverInformation) {
        this.serverInformation = serverInformation;
    }

    public @NotNull Map<String, HiveMQExtension> getEnabledHiveMQExtensions() {
        final Lock lock = extensionsLock.readLock();
        try {
            lock.lock();
            return knownExtensions.values()
                    .stream()
                    .filter(HiveMQExtension::isEnabled)
                    .sorted(Comparator.comparingInt(HiveMQExtension::getPriority))
                    .collect(ImmutableMap.toImmutableMap(HiveMQExtension::getId, Function.identity()));
        } finally {
            lock.unlock();
        }
    }

    public @NotNull ImmutableMap<ClassLoader, HiveMQExtension> getClassloaderToExtensionMap() {
        return ImmutableMap.copyOf(classloaderToExtension);
    }

    public void addHiveMQExtension(final @NotNull HiveMQExtension extension) {
        checkNotNull(extension, "can only add valid extensions");

        final Lock lock = extensionsLock.writeLock();
        try {
            lock.lock();
            final HiveMQExtension oldExtension = knownExtensions.get(extension.getId());
            if (oldExtension != null) {
                extension.setPreviousVersion(oldExtension.getVersion());
            }
            knownExtensions.put(extension.getId(), extension);
        } finally {
            lock.unlock();
        }
    }

    public boolean isHiveMQExtensionIDKnown(final @NotNull String hiveMQExtensionID) {
        checkNotNull(hiveMQExtensionID, "every extension must have an id");

        final Lock lock = extensionsLock.readLock();
        try {
            lock.lock();
            return knownExtensions.containsKey(hiveMQExtensionID);
        } finally {
            lock.unlock();
        }
    }

    public boolean isHiveMQExtensionKnown(
            final @NotNull String hiveMQExtensionID, final @NotNull Path extensionFolder, final boolean enabled) {

        checkNotNull(hiveMQExtensionID, "every extension must have an id");

        final HiveMQExtension extension = getExtension(hiveMQExtensionID, enabled);
        return (extension != null) && extension.getExtensionFolderPath().equals(extensionFolder);
    }

    public boolean isHiveMQExtensionEnabled(@NotNull final String hiveMQExtensionID) {
        checkNotNull(hiveMQExtensionID, "every extension must have an id");

        return getExtension(hiveMQExtensionID, true) != null;
    }

    public @Nullable HiveMQExtension getExtension(final @NotNull String xtensionId, final boolean enabled) {
        final Lock lock = extensionsLock.readLock();
        try {
            lock.lock();
            final HiveMQExtension extension = knownExtensions.get(xtensionId);
            return ((extension == null) || extension.isEnabled() != enabled) ? null : extension;
        } finally {
            lock.unlock();
        }
    }

    public @Nullable HiveMQExtension getExtension(final @NotNull String extensionId) {
        final Lock lock = extensionsLock.readLock();
        try {
            lock.lock();
            return knownExtensions.get(extensionId);
        } finally {
            lock.unlock();
        }
    }

    /**
     * @param classloader the {@link ClassLoader} of the extension.
     * @return null if no extension with this classloader was started or if it was already stopped. Otherwise the
     *         extension associated with this classloader is returned
     */
    public @Nullable HiveMQExtension getExtensionForClassloader(final @NotNull ClassLoader classloader) {
        final Lock lock = classloaderLock.readLock();
        try {
            lock.lock();
            return classloaderToExtension.get(classloader);
        } finally {
            lock.unlock();
        }
    }

    private void addClassLoaderMapping(
            final @NotNull ClassLoader classloader,
            final @NotNull HiveMQExtension extension) {
        final Lock loaderLock = classloaderLock.writeLock();
        try {
            loaderLock.lock();
            if (extension.isEmbedded() && extension.getExtensionMainClazz() != null) {
                //for embedded extensions also add the original (delegate) classloader
                classloaderToExtension.put(extension.getExtensionMainClazz().getClassLoader(), extension);
            }
            classloaderToExtension.put(classloader, extension);
        } finally {
            loaderLock.unlock();
        }
    }

    private void removeClassLoaderMapping(final @NotNull ClassLoader classloader) {
        final Lock loaderLock = classloaderLock.writeLock();
        try {
            loaderLock.lock();
            classloaderToExtension.remove(classloader);
        } finally {
            loaderLock.unlock();
        }
    }

    /**
     * Returns false if the extension is not known to HiveMQ or not enabled
     */
    public boolean extensionStart(@NotNull final String extensionId) {
        checkNotNull(extensionId, "every extension must have an id");

        final HiveMQExtension extension = getExtension(extensionId, true);
        if (extension == null) {
            return false;
        }

        final ClassLoader extensionClassloader = extension.getExtensionClassloader();
        Preconditions.checkNotNull(extensionClassloader, "Extension ClassLoader cannot be null");

        final ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            addClassLoaderMapping(extensionClassloader, extension);

            final ExtensionStartStopInputImpl input =
                    new ExtensionStartStopInputImpl(extension, getEnabledHiveMQExtensions(), serverInformation);
            final ExtensionStartOutputImpl output = new ExtensionStartOutputImpl();

            Thread.currentThread().setContextClassLoader(extensionClassloader);
            extension.start(input, output);

            if (output.getReason().isPresent()) {
                log.info(
                        "Startup of {}extension with id \"{}\" was prevented by the extension itself, reason: {}. Extension will be disabled.",
                        extension.isEmbedded() ? "embedded " : "",
                        extension.getId(),
                        output.getReason().get());
                extensionStartFailed(extension, extensionClassloader);
            } else {
                log.info(
                        "{}xtension \"{}\" version {} started successfully.",
                        extension.isEmbedded() ? "Embedded e" : "E",
                        extension.getName(),
                        extension.getVersion());
                Checkpoints.checkpoint("extension-started");
            }
        } catch (final Throwable t) {
            log.error(
                    "{}xtension with id \"{}\" cannot be started because of an uncaught exception thrown by the extension. Extension will be disabled.",
                    extension.isEmbedded() ? "Embedded e" : "E",
                    extension.getId(),
                    t);
            extensionStartFailed(extension, extensionClassloader);
        } finally {
            Thread.currentThread().setContextClassLoader(previousClassLoader);
        }
        return true;
    }

    private void extensionStartFailed(
            final @NotNull HiveMQExtension extension, final @NotNull ClassLoader extensionClassloader) {

        extension.setDisabled();
        extension.clean(true);
        removeClassLoaderMapping(extensionClassloader);
        Checkpoints.checkpoint("extension-failed");
    }

    /**
     * Returns false if the extension is not known to HiveMQ or not enabled
     */
    public boolean extensionStop(@NotNull final String extensionId, boolean disable) {
        checkNotNull(extensionId, "every extension must have an id");

        final HiveMQExtension extension;

        final Lock lock = extensionsLock.readLock();
        try {
            lock.lock();
            extension = knownExtensions.get(extensionId);
            if ((extension == null) || !extension.isEnabled()) {
                return false;
            }
            extension.setDisabled();
        } finally {
            lock.unlock();
        }

        final ClassLoader extensionClassloader = extension.getExtensionClassloader();
        Preconditions.checkNotNull(extensionClassloader, "Extension ClassLoader cannot be null");

        notifyBeforeExtensionStopCallbacks(extension);

        final ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            final ExtensionStartStopInputImpl input =
                    new ExtensionStartStopInputImpl(extension, getEnabledHiveMQExtensions(), serverInformation);
            final ExtensionStopOutputImpl output = new ExtensionStopOutputImpl();

            Thread.currentThread().setContextClassLoader(extensionClassloader);
            extension.stop(input, output);

            log.info(
                    "{}xtension \"{}\" version {} stopped successfully.",
                    extension.isEmbedded() ? "Embedded e" : "E",
                    extension.getName(),
                    extension.getVersion());
        } catch (final Throwable t) {
            log.warn("Uncaught exception was thrown from extension with id \"" + extension.getId() +
                    "\" on extension stop. Extensions are responsible on their own to handle exceptions.", t);
            disable = true;

        } finally {
            Thread.currentThread().setContextClassLoader(previousClassLoader);

            notifyAfterExtensionStopCallbacks(extension);

            extension.clean(disable);
            removeClassLoaderMapping(extensionClassloader);
            Checkpoints.checkpoint("extension-stopped");
        }
        return true;
    }

    /**
     * Adds a callback that is executed before an extension is stopped.
     *
     * @param callback the consumer of the stopped extension.
     */
    public void addBeforeExtensionStopCallback(final @NotNull Consumer<HiveMQExtension> callback) {
        final Lock lock = beforeExtensionStopCallbacksLock.writeLock();
        try {
            lock.lock();
            beforeExtensionStopCallbacks.add(callback);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Adds a callback that is executed after an extension was stopped.
     *
     * @param callback the consumer of the stopped extension.
     */
    public void addAfterExtensionStopCallback(final @NotNull Consumer<HiveMQExtension> callback) {
        final Lock lock = afterExtensionStopCallbacksLock.writeLock();
        try {
            lock.lock();
            afterExtensionStopCallbacks.add(callback);
        } finally {
            lock.unlock();
        }
    }

    private void notifyBeforeExtensionStopCallbacks(final @NotNull HiveMQExtension extension) {
        final Lock lock = beforeExtensionStopCallbacksLock.readLock();
        try {
            lock.lock();
            for (final Consumer<HiveMQExtension> callback : beforeExtensionStopCallbacks) {
                callback.accept(extension);
            }
        } finally {
            lock.unlock();
        }
    }

    private void notifyAfterExtensionStopCallbacks(final @NotNull HiveMQExtension extension) {
        final Lock lock = afterExtensionStopCallbacksLock.readLock();
        try {
            lock.lock();
            for (final Consumer<HiveMQExtension> callback : afterExtensionStopCallbacks) {
                callback.accept(extension);
            }
        } finally {
            lock.unlock();
        }
    }
}
