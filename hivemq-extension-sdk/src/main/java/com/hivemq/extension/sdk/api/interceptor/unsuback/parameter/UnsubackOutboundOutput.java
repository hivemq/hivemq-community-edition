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

package com.hivemq.extension.sdk.api.interceptor.unsuback.parameter;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.SimpleAsyncOutput;
import com.hivemq.extension.sdk.api.interceptor.unsuback.UnsubackOutboundInterceptor;
import com.hivemq.extension.sdk.api.packets.unsuback.ModifiableUnsubackPacket;

/**
 * This is the output parameter of any {@link UnsubackOutboundInterceptor}.
 *
 * @author Robin Atherton
 */
@DoNotImplement
public interface UnsubackOutboundOutput extends SimpleAsyncOutput<UnsubackOutboundOutput> {

    /**
     * Use this object to make any changes to the outbound UNSUBACK.
     * <p>
     * For MQTT 3 clients you should skip modifying this packet, as the changeable properties only exists since MQTT 5
     * and will therefore not be send to the MQTT 3 client. You can find out the MQTT version via the {@link
     * UnsubackOutboundInput#getConnectionInformation()}.
     *
     * @return A modifiable UNSUBACK packet.
     */
    @NotNull
    ModifiableUnsubackPacket getUnsubackPacket();
}
