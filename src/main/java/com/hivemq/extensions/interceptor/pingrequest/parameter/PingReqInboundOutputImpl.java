package com.hivemq.extensions.interceptor.pingrequest.parameter;

import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.pingrequest.parameter.PingReqInboundOutput;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractSimpleAsyncOutput;
import com.hivemq.extensions.executor.task.PluginTaskOutput;

import java.util.function.Supplier;

/**
 * @author Robin Atherton
 */
public class PingReqInboundOutputImpl extends AbstractSimpleAsyncOutput<PingReqInboundOutput>
        implements PingReqInboundOutput, PluginTaskOutput, Supplier<PingReqInboundOutputImpl> {


    public PingReqInboundOutputImpl(final @NotNull PluginOutPutAsyncer asyncer) {
        super(asyncer);
    }

    @Override
    public @NotNull PingReqInboundOutputImpl get() {
        return this;
    }


}