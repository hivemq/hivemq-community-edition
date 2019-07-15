package com.hivemq.extensions.services.interceptor;

import com.google.common.collect.ImmutableMap;
import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.connack.ConnackOutboundInterceptorProvider;
import com.hivemq.extension.sdk.api.interceptor.connect.ConnectInboundInterceptorProvider;
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
 * @author Florian Limpöck
 */
public class InterceptorsImpl implements Interceptors {

    @NotNull
    private final Map<@NotNull String, @NotNull ConnectInboundInterceptorProvider> connectInboundInterceptorProviderMap;

    @NotNull
    private final Map<@NotNull String, @NotNull ConnackOutboundInterceptorProvider> connackOutboundInterceptorProviderMap;

    @NotNull
    private final HiveMQExtensions hiveMQExtensions;

    @NotNull
    private final ReadWriteLock readWriteLock;

    @Inject
    public InterceptorsImpl(@NotNull final HiveMQExtensions hiveMQExtensions) {
        this.hiveMQExtensions = hiveMQExtensions;
        final PluginPriorityComparator pluginPriorityComparator = new PluginPriorityComparator(hiveMQExtensions);
        this.connectInboundInterceptorProviderMap = new TreeMap<>(pluginPriorityComparator);
        this.connackOutboundInterceptorProviderMap = new TreeMap<>(pluginPriorityComparator);
        this.readWriteLock = new ReentrantReadWriteLock();
        hiveMQExtensions.addAfterExtensionStopCallback(hiveMQExtension -> {
            final IsolatedPluginClassloader pluginClassloader = hiveMQExtension.getPluginClassloader();
            if (pluginClassloader != null) {
                removeInterceptors(hiveMQExtension.getId());
            }
        });

    }

    @Override
    public void addConnectInboundInterceptorProvider(@NotNull final ConnectInboundInterceptorProvider provider) {
        final Lock writeLock = readWriteLock.writeLock();

        writeLock.lock();

        try {

            final IsolatedPluginClassloader pluginClassloader =
                    (IsolatedPluginClassloader) provider.getClass().getClassLoader();

            final HiveMQExtension plugin = hiveMQExtensions.getExtensionForClassloader(pluginClassloader);

            if (plugin != null) {
                connectInboundInterceptorProviderMap.put(plugin.getId(), provider);
            }

        } finally {
            writeLock.unlock();
        }
    }

    @Override
    @NotNull
    public ImmutableMap<String, ConnectInboundInterceptorProvider> connectInboundInterceptorProviders() {
        final Lock readLock = readWriteLock.readLock();

        readLock.lock();
        try {
            return ImmutableMap.copyOf(connectInboundInterceptorProviderMap);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void addConnackOutboundInterceptorProvider(final @NotNull ConnackOutboundInterceptorProvider provider) {
        final Lock writeLock = readWriteLock.writeLock();

        writeLock.lock();

        try {

            final IsolatedPluginClassloader pluginClassloader =
                    (IsolatedPluginClassloader) provider.getClass().getClassLoader();

            final HiveMQExtension plugin = hiveMQExtensions.getExtensionForClassloader(pluginClassloader);

            if (plugin != null) {
                connackOutboundInterceptorProviderMap.put(plugin.getId(), provider);
            }

        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public @NotNull ImmutableMap<String, ConnackOutboundInterceptorProvider> connackOutboundInterceptorProviders() {
        final Lock readLock = readWriteLock.readLock();

        readLock.lock();
        try {
            return ImmutableMap.copyOf(connackOutboundInterceptorProviderMap);
        } finally {
            readLock.unlock();
        }
    }

    private void removeInterceptors(@NotNull final String pluginId) {

        final Lock writeLock = readWriteLock.writeLock();

        writeLock.lock();
        try {
            connectInboundInterceptorProviderMap.remove(pluginId);
            connackOutboundInterceptorProviderMap.remove(pluginId);
        } finally {
            writeLock.unlock();
        }
    }
}
