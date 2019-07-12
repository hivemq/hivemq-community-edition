package com.hivemq.extensions.services.interceptor;

import com.google.common.collect.ImmutableMap;
import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.connect.ConnectInboundInterceptorProvider;

/**
 * @author Lukas Brandl
 * @since 4.2.0
 */
public interface Interceptors {

    /**
     * Add an connect interceptor provider to the connect interceptor provider map
     * If there is already a provider present, it will be overwritten
     *
     * @param provider to be added
     */
    void addConnectInterceptorProvider(@NotNull ConnectInboundInterceptorProvider provider);

    /**
     * Get a map of connect interceptor providers (value) mapped by the id of the plugin which added the interceptor
     * provider (key)
     *
     * @return An immutable copy of the connect interceptor providers
     */
    @NotNull
    ImmutableMap<String, ConnectInboundInterceptorProvider> connectInterceptorProviders();
}
