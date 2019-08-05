/*
 * Copyright 2019 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hivemq.extension.sdk.api.interceptor.connect.parameter;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.AsyncOutput;
import com.hivemq.extension.sdk.api.interceptor.connect.ConnectInboundInterceptor;
import com.hivemq.extension.sdk.api.packets.publish.ModifiableConnectPacket;

/**
 * This is the output parameter of any {@link ConnectInboundInterceptor}
 * providing methods to define the outcome of a CONNECT interception.
 * <p>
 * It can be used to
 * <ul>
 * <li>Modify a CONNECT packet</li>
 * </ul>
 *
 * @author Lukas Brandl
 * @since 4.2.0
 */
@DoNotImplement
public interface ConnectInboundOutput extends AsyncOutput<ConnectInboundOutput> {

    /**
     * Use this object to make any changes to the CONNECT message.
     *
     * @return A modifiable connect packet.
     * @since 4.2.0
     */
    @NotNull ModifiableConnectPacket getConnectPacket();

}
