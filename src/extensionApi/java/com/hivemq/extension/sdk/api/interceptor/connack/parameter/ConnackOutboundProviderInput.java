package com.hivemq.extension.sdk.api.interceptor.connack.parameter;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extension.sdk.api.interceptor.connack.ConnackOutboundInterceptorProvider;
import com.hivemq.extension.sdk.api.parameter.ClientBasedInput;

/**
 * This is the input parameter of any {@link ConnackOutboundInterceptorProvider}
 * providing {@link ServerInformation} and {@link ClientBasedInput}.
 *
 * @author Lukas Brandl
 * @since 4.2.0
 */
public interface ConnackOutboundProviderInput extends ClientBasedInput {

    /**
     * Get information about the HiveMQ instance the extension is running in.
     *
     * @return The {@link ServerInformation} of the input.
     * @since 4.2.0
     */
    @NotNull ServerInformation getServerInformation();
}
