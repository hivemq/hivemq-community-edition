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

package com.hivemq.extension.sdk.api.auth.parameter;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.EnhancedAuthenticator;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.packets.connect.ConnectPacket;
import com.hivemq.extension.sdk.api.parameter.ClientBasedInput;

/**
 * The input object of any {@link EnhancedAuthenticator}
 * providing an unmodifiable {@link ConnectPacket}, {@link ClientBasedInput} and further information on the connection as {@link ConnectionInformation}.
 *
 * @author Daniel Krüger
 * @author Florian Limpöck
 *
*/
@DoNotImplement
public interface ConnectEnhancedAuthInput extends ClientBasedInput {

    /**
     * Get the unmodifiable CONNECT packet for the MQTT client that has to be authenticated.
     *
     * @return The {@link ConnectPacket} of the input.
    */
    @Immutable
    @NotNull ConnectPacket getConnectPacket();
}
