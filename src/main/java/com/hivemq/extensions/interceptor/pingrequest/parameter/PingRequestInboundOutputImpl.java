package com.hivemq.extensions.interceptor.pingrequest.parameter;

import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.pingrequest.parameter.PingRequestInboundOutput;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractSimpleAsyncOutput;
import com.hivemq.extensions.executor.task.PluginTaskOutput;

import java.util.function.Supplier;

/**
 * @author Robin Atherton
 */
public class PingRequestInboundOutputImpl extends AbstractSimpleAsyncOutput<PingRequestInboundOutput>
        implements PingRequestInboundOutput, PluginTaskOutput, Supplier<PingRequestInboundOutputImpl> {


    public PingRequestInboundOutputImpl(final @NotNull PluginOutPutAsyncer asyncer) {
        super(asyncer);
    }

    @Override
    public PingRequestInboundOutputImpl get() {
        return this;
    }


}