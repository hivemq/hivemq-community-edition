package com.hivemq.extensions.interceptor.unsubscribe.parameter;

import com.hivemq.annotations.NotNull;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.interceptor.unsubscribe.parameter.UnsubscribeInboundOutput;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractAsyncOutput;
import com.hivemq.extensions.executor.task.PluginTaskOutput;
import com.hivemq.extensions.packets.unsubscribe.ModifiableUnsubscribePacketImpl;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;

import java.util.function.Supplier;

/**
 * @author Robin Atherton
 */
public class UnsubscribeInboundOutputImpl extends AbstractAsyncOutput<UnsubscribeInboundOutput>
        implements UnsubscribeInboundOutput, PluginTaskOutput, Supplier<UnsubscribeInboundOutputImpl> {

    private final @NotNull FullConfigurationService configurationService;
    private @NotNull ModifiableUnsubscribePacketImpl unsubscribePacket;

    public UnsubscribeInboundOutputImpl(
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull FullConfigurationService configurationService,
            final @NotNull UNSUBSCRIBE unsubscribe) {
        super(asyncer);
        this.configurationService = configurationService;
        this.unsubscribePacket = new ModifiableUnsubscribePacketImpl(configurationService, unsubscribe);
    }

    @Override
    public @NotNull ModifiableUnsubscribePacketImpl getUnsubscribePacket() {
        return this.unsubscribePacket;
    }

    @Override
    public UnsubscribeInboundOutputImpl get() {
        return this;
    }

    public void update(final @NotNull ModifiableUnsubscribePacketImpl unsubscribePacket) {
        this.unsubscribePacket = unsubscribePacket;
    }

    public void update(final @NotNull UNSUBSCRIBE unsubscribe) {
        this.unsubscribePacket = new ModifiableUnsubscribePacketImpl(configurationService, unsubscribe);
    }


}
