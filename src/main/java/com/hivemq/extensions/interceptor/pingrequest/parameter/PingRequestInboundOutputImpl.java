package com.hivemq.extensions.interceptor.pingrequest.parameter;

import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.pingrequest.parameter.PingRequestInboundOutput;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractAsyncOutput;
import com.hivemq.extensions.executor.task.PluginTaskOutput;
import com.hivemq.mqtt.message.PINGREQ;

import java.util.function.Supplier;

/**
 * @author Robin Atherton
 */
public class PingRequestInboundOutputImpl extends AbstractAsyncOutput<PingRequestInboundOutput>
        implements PingRequestInboundOutput, PluginTaskOutput, Supplier<PingRequestInboundOutputImpl> {

    private final @NotNull PINGREQ pingreq;

    public PingRequestInboundOutputImpl(final @NotNull PluginOutPutAsyncer asyncer) {
        super(asyncer);
        this.pingreq = new PINGREQ();
    }

    public PINGREQ getPingreq() {
        return pingreq;
    }
    @Override
    public PingRequestInboundOutputImpl get() {
        return this;
    }


}