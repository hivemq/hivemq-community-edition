package com.hivemq.extensions.interceptor.pingrequestresponse;

import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.pingrequestresponse.parameter.PingRequestResponseOutput;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractAsyncOutput;
import com.hivemq.extensions.executor.task.PluginTaskOutput;
import com.hivemq.mqtt.message.PINGREQ;
import com.hivemq.mqtt.message.PINGRESP;

import java.util.function.Supplier;

/**
 * @author Robin Atherton
 */
public class PingRequestResponseOutputImpl extends AbstractAsyncOutput<PingRequestResponseOutput>
        implements Supplier<PingRequestResponseOutputImpl>, PingRequestResponseOutput,
        PluginTaskOutput {

    private final PINGRESP pingResp;
    private final PINGREQ pingReq;

    public PingRequestResponseOutputImpl(
            final @NotNull PluginOutPutAsyncer asyncer) {
        super(asyncer);
        pingResp = new PINGRESP();
        pingReq = new PINGREQ();
    }

    public PINGRESP getPingResp() {
        return pingResp;
    }

    public PINGREQ getPingReq() {
        return pingReq;
    }

    @Override
    public PingRequestResponseOutputImpl get() {
        return this;
    }
}
