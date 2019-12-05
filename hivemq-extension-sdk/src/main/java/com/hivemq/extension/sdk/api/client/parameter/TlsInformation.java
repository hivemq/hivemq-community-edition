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
package com.hivemq.extension.sdk.api.client.parameter;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.security.cert.X509Certificate;

/**
 * The TLS information contains specific data about the TLS connection, should the client use TLS.
 *
 * @author Christoph Schäbel
 * @since 4.0.0
 */
@DoNotImplement
public interface TlsInformation {

    /**
     * @return The certificate.
     * @since 4.0.0
     */
    @NotNull X509Certificate getCertificate();

    /**
     * @return The certificate chain.
     * @since 4.0.0
     */
    @NotNull X509Certificate[] getCertificateChain();

    /**
     * @return The cipher suite.
     * @since 4.0.0
     */
    @NotNull String getCipherSuite();

    /**
     * @return The protocol.
     * @since 4.0.0
     */
    @NotNull String getProtocol();
}
