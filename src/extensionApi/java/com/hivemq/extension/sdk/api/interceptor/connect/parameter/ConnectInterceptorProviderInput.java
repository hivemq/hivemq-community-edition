package com.hivemq.extension.sdk.api.interceptor.connect.parameter;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extension.sdk.api.parameter.ClientBasedInput;
import com.hivemq.extension.sdk.api.interceptor.connect.ConnectInterceptorProvider;

/**
 * This is the input parameter of any {@link ConnectInterceptorProvider}
 * providing {@link ServerInformation} and {@link ClientBasedInput}.
 *
 * @author Lukas Brandl
 * @since 4.2.0
 */
public interface ConnectInterceptorProviderInput extends ClientBasedInput {

    /**
     * Get information about the HiveMQ instance the extension is running in.
     *
     * @return The {@link ServerInformation} of the input.
     * @since 4.2.0
     */
    @NotNull ServerInformation getServerInformation();
}
