package com.hivemq.extensions.interceptor.connack;

import com.hivemq.annotations.NotNull;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.interceptor.connack.parameter.ConnackOutboundOutput;
import com.hivemq.extension.sdk.api.packets.connack.ModifiableConnackPacket;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractAsyncOutput;
import com.hivemq.extensions.executor.task.PluginTaskOutput;
import com.hivemq.extensions.packets.connack.ModifiableConnackPacketImpl;
import com.hivemq.mqtt.message.connack.CONNACK;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * @author Florian Limpöck
 * @since 4.2.0
 */
public class ConnackOutboundOutputImpl extends AbstractAsyncOutput<ConnackOutboundOutput> implements ConnackOutboundOutput, PluginTaskOutput, Supplier<ConnackOutboundOutputImpl> {

    private final @NotNull ModifiableConnackPacketImpl connackPacket;
    private final @NotNull AtomicBoolean connackPrevented = new AtomicBoolean(false);

    public ConnackOutboundOutputImpl(final @NotNull FullConfigurationService configurationService, final @NotNull PluginOutPutAsyncer asyncer, final @NotNull CONNACK connack, final boolean requestResponseInformation) {
        super(asyncer);
        this.connackPacket = new ModifiableConnackPacketImpl(configurationService, connack, requestResponseInformation);
    }

    @Override
    public @NotNull ModifiableConnackPacket getConnackPacket() {
        return connackPacket;
    }

    @Override
    public @NotNull ConnackOutboundOutputImpl get() {
        return this;
    }

    public void forciblyDisconnect() {
        this.connackPrevented.set(true);
    }

    public boolean connackPrevented() {
        return connackPrevented.get();
    }
}
