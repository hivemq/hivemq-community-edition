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

package com.hivemq.extensions.client.parameter;

import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.parameter.AuthenticatorProviderInput;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extensions.auth.parameter.AuthenticatorProviderInputImpl;
import io.netty.channel.ChannelHandlerContext;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Christoph Schäbel
 */
@Singleton
public class AuthenticatorProviderInputFactory {

    @NotNull
    private final ServerInformation serverInformation;

    @Inject
    public AuthenticatorProviderInputFactory(@NotNull final ServerInformation serverInformation) {
        this.serverInformation = serverInformation;
    }

    @NotNull
    public AuthenticatorProviderInput createInput(@NotNull final ChannelHandlerContext ctx, final @NotNull String clientId) {
        return new AuthenticatorProviderInputImpl(serverInformation, ctx.channel(), clientId);
    }

}
