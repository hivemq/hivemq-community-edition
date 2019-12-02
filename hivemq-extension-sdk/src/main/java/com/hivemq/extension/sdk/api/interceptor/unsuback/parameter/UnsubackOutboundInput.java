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
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.unsuback.UnsubackOutboundInterceptor;
import com.hivemq.extension.sdk.api.packets.unsuback.UnsubackPacket;
import com.hivemq.extension.sdk.api.parameter.ClientBasedInput;

/**
 * This is the input parameter of any {@link UnsubackOutboundInterceptor} providing UNSUBACK, connection and client
 * based information.
 *
 * @author Robin Atherton
 */
@DoNotImplement
public interface UnsubackOutboundInput extends ClientBasedInput {

    /**
     * The unmodifiable UNSUBACK packet that was intercepted.
     *
     * @return An unmodifiable {@link UnsubackPacket}.
     */
    @NotNull
    @Immutable
    UnsubackPacket getUnsubackPacket();
}
