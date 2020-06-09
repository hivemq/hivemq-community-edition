/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.extensions.interceptor.connect.parameter;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.interceptor.connect.parameter.ConnectInboundOutput;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractAsyncOutput;
import com.hivemq.extensions.packets.connect.ModifiableConnectPacketImpl;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Lukas Brandl
 * @author Silvio Giebl
 * @since 4.2.0
 */
public class ConnectInboundOutputImpl extends AbstractAsyncOutput<ConnectInboundOutput>
        implements ConnectInboundOutput {

    private final @NotNull ModifiableConnectPacketImpl connectPacket;
    private final @NotNull AtomicBoolean prevent = new AtomicBoolean(false);
    private @Nullable String reasonString;
    private @Nullable String logMessage;

    public ConnectInboundOutputImpl(
            final @NotNull PluginOutPutAsyncer asyncer, final @NotNull ModifiableConnectPacketImpl connectPacket) {

        super(asyncer);
        this.connectPacket = connectPacket;
    }

    @Override
    public @NotNull ModifiableConnectPacketImpl getConnectPacket() {
        return connectPacket;
    }

    public void prevent(final @NotNull String reasonString, final @NotNull String logMessage) {
        prevent.set(true);
        this.reasonString = reasonString;
        this.logMessage = logMessage;
    }

    public boolean isPrevent() {
        return prevent.get();
    }

    public @Nullable String getReasonString() {
        return reasonString;
    }

    public @Nullable String getLogMessage() {
        return logMessage;
    }

    public @NotNull ConnectInboundOutputImpl update(final @NotNull ConnectInboundInputImpl input) {
        return new ConnectInboundOutputImpl(asyncer, connectPacket.update(input.getConnectPacket()));
    }
}
