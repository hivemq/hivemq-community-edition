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
package com.hivemq.extensions.interceptor.pingreq.parameter;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.pingreq.parameter.PingReqInboundOutput;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.AbstractAsyncOutput;

import java.util.function.Supplier;

/**
 * @author Robin Atherton
 */
public class PingReqInboundOutputImpl extends AbstractAsyncOutput<PingReqInboundOutput>
        implements PingReqInboundOutput, Supplier<PingReqInboundOutputImpl> {

    public PingReqInboundOutputImpl(final @NotNull PluginOutPutAsyncer asyncer) {
        super(asyncer);
    }

    @Override
    public @NotNull PingReqInboundOutputImpl get() {
        return this;
    }
}
