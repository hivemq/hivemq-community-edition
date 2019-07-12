package com.hivemq.extensions.services.interceptor;

import com.google.common.base.Preconditions;
import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.connect.ConnectInboundInterceptorProvider;
import com.hivemq.extension.sdk.api.services.interceptor.GlobalInterceptorRegistry;

import javax.inject.Inject;

/**
 * @author Lukas Brandl
 * @since 4.2.0
 */
public class GlobalInterceptorRegistryImpl implements GlobalInterceptorRegistry {

    @NotNull
    private final Interceptors interceptors;

    @Inject
    public GlobalInterceptorRegistryImpl(@NotNull final Interceptors interceptors) {
        this.interceptors = interceptors;
    }

    @Override
    public void setConnectInterceptorProvider(@NotNull final ConnectInboundInterceptorProvider connectInboundInterceptorProvider) {
        Preconditions.checkNotNull(connectInboundInterceptorProvider, "Connect interceptor provider must never be null");
        interceptors.addConnectInterceptorProvider(connectInboundInterceptorProvider);
    }
}
