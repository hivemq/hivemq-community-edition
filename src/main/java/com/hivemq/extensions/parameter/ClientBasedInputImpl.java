package com.hivemq.extensions.parameter;

import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.parameter.ClientBasedInput;
import com.hivemq.extensions.PluginInformationUtil;
import com.hivemq.extensions.executor.task.PluginTaskInput;
import io.netty.channel.Channel;

/**
 * @author Florian Limpöck
 * @author Silvio Giebl
 */
public abstract class ClientBasedInputImpl implements ClientBasedInput, PluginTaskInput {

    private final @NotNull ClientInformation clientInformation;
    private final @NotNull ConnectionInformation connectionInformation;

    public ClientBasedInputImpl(final @NotNull String clientId, final @NotNull Channel channel) {
        clientInformation = PluginInformationUtil.getAndSetClientInformation(channel, clientId);
        connectionInformation = PluginInformationUtil.getAndSetConnectionInformation(channel);
    }

    public @NotNull ClientInformation getClientInformation() {
        return clientInformation;
    }

    public @NotNull ConnectionInformation getConnectionInformation() {
        return connectionInformation;
    }
}
