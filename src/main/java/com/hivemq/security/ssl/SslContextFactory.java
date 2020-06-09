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
package com.hivemq.security.ssl;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.configuration.service.entity.Tls;
import io.netty.handler.ssl.SslContext;

import javax.net.ssl.SSLException;

public interface SslContextFactory {


    /**
     * Creates a new {@link SslContext} according to the information stored in the {@link Tls} object
     *
     * @param tls the Tls object with the information for the Tls connection
     * @return the {@link SslContext}
     * @throws SSLException thrown if the SslContext could not be created
     */
    @NotNull
    SslContext createSslContext(@NotNull final Tls tls) throws SSLException;

}
