package com.hivemq.extensions.services.interceptor;

import com.google.common.collect.ImmutableMap;
import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.connect.ConnectInterceptor;
import com.hivemq.extension.sdk.api.interceptor.connect.ConnectInterceptorProvider;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.PluginPriorityComparator;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;

import javax.inject.Inject;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Lukas Brandl
 */
public class InterceptorsImpl implements Interceptors {

    @NotNull
    private final Map<@NotNull String, @NotNull ConnectInterceptorProvider> connectInterceptorMap;

    @NotNull
    private final HiveMQExtensions hiveMQExtensions;

    @NotNull
    private final ReadWriteLock readWriteLock;

    @Inject
    public InterceptorsImpl(@NotNull final HiveMQExtensions hiveMQExtensions) {
        this.hiveMQExtensions = hiveMQExtensions;
        this.connectInterceptorMap = new TreeMap<>(new PluginPriorityComparator(hiveMQExtensions));
        this.readWriteLock = new ReentrantReadWriteLock();
        hiveMQExtensions.addAfterExtensionStopCallback(hiveMQExtension -> {
            final IsolatedPluginClassloader pluginClassloader = hiveMQExtension.getPluginClassloader();
            if (pluginClassloader != null) {
                removeConnectInterceptor(hiveMQExtension.getId());
            }
        });

    }

    @Override
    public void addConnectInterceptorProvider(@NotNull final ConnectInterceptorProvider provider) {
        final Lock writeLock = readWriteLock.writeLock();

        writeLock.lock();

        try {

            final IsolatedPluginClassloader pluginClassloader =
                    (IsolatedPluginClassloader) provider.getClass().getClassLoader();

            final HiveMQExtension plugin = hiveMQExtensions.getExtensionForClassloader(pluginClassloader);

            if (plugin != null) {
                connectInterceptorMap.put(plugin.getId(), provider);
            }

        } finally {
            writeLock.unlock();
        }
    }

    @Override
    @NotNull
    public ImmutableMap<String, ConnectInterceptorProvider> connectInterceptorProviders() {
        final Lock readLock = readWriteLock.readLock();

        readLock.lock();
        try {
            return ImmutableMap.copyOf(connectInterceptorMap);
        } finally {
            readLock.unlock();
        }
    }

    private void removeConnectInterceptor(@NotNull final String pluginId) {

        final Lock writeLock = readWriteLock.writeLock();

        writeLock.lock();
        try {
            connectInterceptorMap.remove(pluginId);
        } finally {
            writeLock.unlock();
        }
    }
}
