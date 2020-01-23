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

import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.parameter.ClientBasedInput;
import com.hivemq.extension.sdk.api.services.auth.provider.AuthenticatorProvider;

/**
 * This is the input parameter of any {@link AuthenticatorProvider}
 * providing {@link ServerInformation} and {@link ClientBasedInput}.
 *
 * @author Georg Held
 * @author Florian Limpöck
 * @since 4.0.0
 */
@DoNotImplement
public interface AuthenticatorProviderInput extends ClientBasedInput {

    /**
     * Get information about the HiveMQ instance the extension is running in.
     *
     * @return The {@link ServerInformation} of the input.
     * @since 4.0.0
     */
    @NotNull ServerInformation getServerInformation();
}
