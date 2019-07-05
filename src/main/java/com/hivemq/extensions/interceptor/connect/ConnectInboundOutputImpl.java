package com.hivemq.extensions.interceptor.connect;

import com.hivemq.annotations.NotNull;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.interceptor.connect.parameter.ConnectInboundOutput;
import com.hivemq.extension.sdk.api.packets.publish.ModifiableConnectPacket;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractAsyncOutput;
import com.hivemq.extensions.executor.task.PluginTaskOutput;
import com.hivemq.extensions.packets.connect.ModifiableConnectPacketImpl;
import com.hivemq.mqtt.message.connect.CONNECT;

import java.util.function.Supplier;

/**
 * @author Lukas Brandl
 * @since 4.2.0
 */
public class ConnectInboundOutputImpl extends AbstractAsyncOutput<ConnectInboundOutput> implements ConnectInboundOutput, PluginTaskOutput, Supplier<ConnectInboundOutputImpl> {

    private final @NotNull ModifiableConnectPacketImpl connectPacket;

    public ConnectInboundOutputImpl(final @NotNull FullConfigurationService configurationService, final @NotNull PluginOutPutAsyncer asyncer, final @NotNull CONNECT connect) {
        super(asyncer);
        this.connectPacket = new ModifiableConnectPacketImpl(configurationService, connect);
    }

    @Override
    public @NotNull ModifiableConnectPacket getConnectPacket() {
        return connectPacket;
    }


    @Override
    public @NotNull ConnectInboundOutputImpl get() {
        return this;
    }

}
