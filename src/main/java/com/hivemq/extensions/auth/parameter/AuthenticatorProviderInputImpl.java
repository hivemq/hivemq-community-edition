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
package com.hivemq.extensions.auth.parameter;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.parameter.AuthenticatorProviderInput;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extensions.parameter.ClientBasedInputImpl;
import io.netty.channel.Channel;

/**
 * @author Georg Held
 */
public class AuthenticatorProviderInputImpl extends ClientBasedInputImpl implements AuthenticatorProviderInput {

    private final @NotNull ServerInformation serverInformation;

    public AuthenticatorProviderInputImpl(
            final @NotNull ServerInformation serverInformation,
            final @NotNull Channel channel,
            final @NotNull String clientId) {

        super(clientId, channel);
        this.serverInformation = serverInformation;
    }

    @Override
    public @NotNull ServerInformation getServerInformation() {
        return serverInformation;
    }
}
