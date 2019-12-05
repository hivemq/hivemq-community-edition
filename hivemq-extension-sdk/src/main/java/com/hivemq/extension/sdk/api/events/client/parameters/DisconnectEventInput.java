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
package com.hivemq.extension.sdk.api.events.client.parameters;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.events.client.ClientLifecycleEventListener;
import com.hivemq.extension.sdk.api.packets.general.DisconnectedReasonCode;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extension.sdk.api.parameter.ClientBasedInput;

import java.util.Optional;

/**
 * Input object for {@link ClientLifecycleEventListener} methods for disconnect
 * events.
 *
 * @author Christoph Schäbel
 * @since 4.0.0
 */
@DoNotImplement
public interface DisconnectEventInput extends ClientBasedInput {

    /**
     * @return An {@link Optional} containing the disconnected reason code if present.
     * @since 4.0.0
     */
    @NotNull
    Optional<DisconnectedReasonCode> getReasonCode();

    /**
     * @return An {@link Optional} containing the disconnect reason string if present.
     * @since 4.0.0
     */
    @NotNull
    Optional<String> getReasonString();

    /**
     * @return An {@link Optional} containing the disconnect user properties if present.
     * @since 4.0.0
     */
    @NotNull
    Optional<UserProperties> getUserProperties();
}
