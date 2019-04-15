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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.hivemq.annotations.NotNull;
import com.hivemq.annotations.Nullable;
import com.hivemq.annotations.ThreadSafe;
import com.hivemq.common.annotations.GuardedBy;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.parameters.start.ExtensionStartOutputImpl;
import com.hivemq.extensions.parameters.start.ExtensionStartStopInputImpl;
import com.hivemq.extensions.parameters.stop.ExtensionStopOutputImpl;
import com.hivemq.util.Checkpoints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class HiveMQPlugins {

    private static final Logger log = LoggerFactory.getLogger(HiveMQPlugins.class);

    @GuardedBy("pluginsLock")
    private final @NotNull HashMap<String, HiveMQExtension> knownPlugins = new HashMap<>();
    @GuardedBy("classloaderLock")
    private final @NotNull HashMap<IsolatedPluginClassloader, HiveMQExtension> classloaderToPlugin = new HashMap<>();
    @GuardedBy("beforePluginStopCallbacksLock")
    private final @NotNull List<Consumer<HiveMQExtension>> beforePluginStopCallbacks = new LinkedList<>();
    @GuardedBy("afterPluginStopCallbacksLock")
    private final @NotNull List<Consumer<HiveMQExtension>> afterPluginStopCallbacks = new LinkedList<>();

    private final @NotNull ReadWriteLock pluginsLock = new ReentrantReadWriteLock();
    private final @NotNull ReadWriteLock classloaderLock = new ReentrantReadWriteLock();
    private final @NotNull ReadWriteLock beforePluginStopCallbacksLock = new ReentrantReadWriteLock();
    private final @NotNull ReadWriteLock afterPluginStopCallbacksLock = new ReentrantReadWriteLock();

    @NotNull
    public Map<String, HiveMQExtension> getEnabledHiveMQPlugins() {
        final Lock lock = pluginsLock.readLock();
        try {
            lock.lock();
            return knownPlugins.values().stream()
                    .filter(HiveMQExtension::isEnabled)
                    .sorted(Comparator.comparingInt(HiveMQExtension::getPriority))
                    .collect(ImmutableMap.toImmutableMap(HiveMQExtension::getId, Function.identity()));
        } finally {
            lock.unlock();
        }
    }

    public @NotNull ImmutableMap<IsolatedPluginClassloader, HiveMQExtension> getClassloaderToPluginMap() {
        return ImmutableMap.copyOf(classloaderToPlugin);
    }

    public void addHiveMQPlugin(final @NotNull HiveMQExtension plugin) {
        checkNotNull(plugin, "can only add valid extensions");

        final Lock lock = pluginsLock.writeLock();
        try {
            lock.lock();
            final HiveMQExtension oldPlugin = knownPlugins.get(plugin.getId());
            if (oldPlugin != null) {
                plugin.setPreviousVersion(oldPlugin.getVersion());
            }
            knownPlugins.put(plugin.getId(), plugin);
        } finally {
            lock.unlock();
        }
    }

    public boolean isHiveMQPluginIDKnown(final @NotNull String hiveMQPluginID) {
        checkNotNull(hiveMQPluginID, "every extension must have an id");

        final Lock lock = pluginsLock.readLock();
        try {
            lock.lock();
            return knownPlugins.containsKey(hiveMQPluginID);
        } finally {
            lock.unlock();
        }
    }

    public boolean isHiveMQPluginKnown(
            final @NotNull String hiveMQPluginID, final @NotNull Path pluginFolder, final boolean enabled) {

        checkNotNull(hiveMQPluginID, "every extension must have an id");

        final HiveMQExtension plugin = getPlugin(hiveMQPluginID, enabled);
        return (plugin != null) && plugin.getPluginFolderPath().equals(pluginFolder);
    }

    public boolean isHiveMQPluginEnabled(@NotNull final String hiveMQPluginID) {
        checkNotNull(hiveMQPluginID, "every extension must have an id");

        return getPlugin(hiveMQPluginID, true) != null;
    }

    public @Nullable HiveMQExtension getPlugin(final @NotNull String pluginId, final boolean enabled) {
        final Lock lock = pluginsLock.readLock();
        try {
            lock.lock();
            final HiveMQExtension plugin = knownPlugins.get(pluginId);
            return ((plugin == null) || plugin.isEnabled() != enabled) ? null : plugin;
        } finally {
            lock.unlock();
        }
    }

    public @Nullable HiveMQExtension getPlugin(final @NotNull String pluginId) {
        final Lock lock = pluginsLock.readLock();
        try {
            lock.lock();
            return knownPlugins.get(pluginId);
        } finally {
            lock.unlock();
        }
    }

    /**
     * @param classloader a {@link IsolatedPluginClassloader}
     * @return null if no extension with this classloader was started or if it was already stopped. Otherwise the extension
     * associated with this classloader is returned
     */
    public @Nullable HiveMQExtension getPluginForClassloader(final @NotNull IsolatedPluginClassloader classloader) {
        final Lock lock = classloaderLock.readLock();
        try {
            lock.lock();
            return classloaderToPlugin.get(classloader);
        } finally {
            lock.unlock();
        }
    }

    private void addClassLoaderMapping(
            final @NotNull IsolatedPluginClassloader pluginClassloader, final @NotNull HiveMQExtension plugin) {

        final Lock loaderLock = classloaderLock.writeLock();
        try {
            loaderLock.lock();
            classloaderToPlugin.put(pluginClassloader, plugin);
        } finally {
            loaderLock.unlock();
        }
    }

    private void removeClassLoaderMapping(final @NotNull IsolatedPluginClassloader pluginClassloader) {
        final Lock loaderLock = classloaderLock.writeLock();
        try {
            loaderLock.lock();
            classloaderToPlugin.remove(pluginClassloader);
        } finally {
            loaderLock.unlock();
        }
    }

    /**
     * Returns false if the extension is not known to HiveMQ or not enabled
     */
    public boolean pluginStart(@NotNull final String pluginId) {
        checkNotNull(pluginId, "every extension must have an id");

        final HiveMQExtension plugin = getPlugin(pluginId, true);
        if (plugin == null) {
            return false;
        }

        final IsolatedPluginClassloader pluginClassloader = plugin.getPluginClassloader();
        Preconditions.checkNotNull(pluginClassloader, "Extension ClassLoader cannot be null");

        final ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            addClassLoaderMapping(pluginClassloader, plugin);

            final ExtensionStartStopInputImpl input = new ExtensionStartStopInputImpl(plugin, getEnabledHiveMQPlugins());
            final ExtensionStartOutputImpl output = new ExtensionStartOutputImpl();

            Thread.currentThread().setContextClassLoader(pluginClassloader);
            plugin.start(input, output);

            if (output.getReason().isPresent()) {
                log.info(
                        "Startup of extension with id \"{}\" was prevented by the extension itself, reason: {}. Extension will be disabled.",
                        plugin.getId(), output.getReason().get());
                pluginStartFailed(plugin, pluginClassloader);
            } else {
                log.info("Extension \"{}\" version {} started successfully.", plugin.getName(), plugin.getVersion());
                Checkpoints.checkpoint("extension-started");
            }

        } catch (final Throwable t) {
            log.error(
                    "Extension with id \"{}\" cannot be started because of an uncaught exception thrown by the extension. Extension will be disabled.",
                    plugin.getId(), t);
            pluginStartFailed(plugin, pluginClassloader);

        } finally {
            Thread.currentThread().setContextClassLoader(previousClassLoader);
        }
        return true;
    }

    private void pluginStartFailed(
            final @NotNull HiveMQExtension plugin, final @NotNull IsolatedPluginClassloader pluginClassloader) {

        plugin.setDisabled();
        plugin.clean(true);
        removeClassLoaderMapping(pluginClassloader);
        Checkpoints.checkpoint("extension-failed");
    }

    /**
     * Returns false if the extension is not known to HiveMQ or not enabled
     */
    public boolean pluginStop(@NotNull final String pluginId, final boolean disable) {
        checkNotNull(pluginId, "every extension must have an id");

        final HiveMQExtension plugin;

        final Lock lock = pluginsLock.readLock();
        try {
            lock.lock();
            plugin = knownPlugins.get(pluginId);
            if ((plugin == null) || !plugin.isEnabled()) {
                return false;
            }
            plugin.setDisabled();
        } finally {
            lock.unlock();
        }

        final IsolatedPluginClassloader pluginClassloader = plugin.getPluginClassloader();
        Preconditions.checkNotNull(pluginClassloader, "Extension ClassLoader cannot be null");

        notifyBeforePluginStopCallbacks(plugin);

        final ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            final ExtensionStartStopInputImpl input = new ExtensionStartStopInputImpl(plugin, getEnabledHiveMQPlugins());
            final ExtensionStopOutputImpl output = new ExtensionStopOutputImpl();

            Thread.currentThread().setContextClassLoader(pluginClassloader);
            plugin.stop(input, output);

            log.info("Extension \"{}\" version {} stopped successfully.", plugin.getName(), plugin.getVersion());

        } catch (final Throwable t) {
            log.warn("Uncaught exception was thrown from extension with id \"" + plugin.getId() + "\" on extension stop. " +
                    "Extensions are responsible on their own to handle exceptions.", t);

        } finally {
            Thread.currentThread().setContextClassLoader(previousClassLoader);

            notifyAfterPluginStopCallbacks(plugin);

            plugin.clean(disable);
            removeClassLoaderMapping(pluginClassloader);
            Checkpoints.checkpoint("extension-stopped");
        }
        return true;
    }

    /**
     * Adds a callback that is executed before an extension is stopped.
     *
     * @param callback the consumer of the stopped extension.
     */
    public void addBeforePluginStopCallback(final @NotNull Consumer<HiveMQExtension> callback) {
        final Lock lock = beforePluginStopCallbacksLock.writeLock();
        try {
            lock.lock();
            beforePluginStopCallbacks.add(callback);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Adds a callback that is executed after an extension was stopped.
     *
     * @param callback the consumer of the stopped extension.
     */
    public void addAfterPluginStopCallback(final @NotNull Consumer<HiveMQExtension> callback) {
        final Lock lock = afterPluginStopCallbacksLock.writeLock();
        try {
            lock.lock();
            afterPluginStopCallbacks.add(callback);
        } finally {
            lock.unlock();
        }
    }

    private void notifyBeforePluginStopCallbacks(final @NotNull HiveMQExtension plugin) {
        final Lock lock = beforePluginStopCallbacksLock.readLock();
        try {
            lock.lock();
            for (final Consumer<HiveMQExtension> callback : beforePluginStopCallbacks) {
                callback.accept(plugin);
            }
        } finally {
            lock.unlock();
        }
    }

    private void notifyAfterPluginStopCallbacks(final @NotNull HiveMQExtension plugin) {
        final Lock lock = afterPluginStopCallbacksLock.readLock();
        try {
            lock.lock();
            for (final Consumer<HiveMQExtension> callback : afterPluginStopCallbacks) {
                callback.accept(plugin);
            }
        } finally {
            lock.unlock();
        }
    }
}
