package com.hivemq.extensions.interceptor.pingresponse.parameter;

import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.pingresponse.parameter.PingResponseOutboundOutput;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractSimpleAsyncOutput;
import com.hivemq.extensions.executor.task.PluginTaskOutput;

import java.util.function.Supplier;

/**
 * @author Robin Atherton
 */
public class PingResponseOutboundOutputImpl extends AbstractSimpleAsyncOutput<PingResponseOutboundOutput>
        implements PingResponseOutboundOutput, PluginTaskOutput, Supplier<PingResponseOutboundOutputImpl> {


    public PingResponseOutboundOutputImpl(final @NotNull PluginOutPutAsyncer asyncer) {
        super(asyncer);
    }

    @Override
    public PingResponseOutboundOutputImpl get() {
        return this;
    }
}
