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
package com.hivemq.extensions.client.parameter;

import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.client.parameter.ClientTlsInformation;
import com.hivemq.extension.sdk.api.client.parameter.TlsInformation;

import java.security.cert.X509Certificate;
import java.util.Optional;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class ClientTlsInformationImpl implements ClientTlsInformation, TlsInformation {

    private final @Nullable X509Certificate certificate;
    private final X509Certificate @Nullable [] certificateChain;
    private final @NotNull String cipherSuite;
    private final @NotNull String protocol;
    private final @Nullable String hostname;

    public ClientTlsInformationImpl(final @Nullable X509Certificate certificate,
                                    final X509Certificate @Nullable [] certificateChain,
                                    final @NotNull String cipherSuite,
                                    final @NotNull String protocol,
                                    final @Nullable String hostname) {
        Preconditions.checkNotNull(cipherSuite, "cipher suite must never be null");
        Preconditions.checkNotNull(protocol, "protocol must never be null");
        this.certificate = certificate;
        this.certificateChain = certificateChain;
        this.cipherSuite = cipherSuite;
        this.protocol = protocol;
        this.hostname = hostname;
    }


    //legacy method, now deprecated
    @Override
    public @NotNull X509Certificate getCertificate() {
        Preconditions.checkNotNull(certificate, "certificate must never be null");
        return certificate;
    }

    //legacy method, now deprecated
    @Override
    public @NotNull X509Certificate[] getCertificateChain() {
        Preconditions.checkNotNull(certificateChain, "certificate chain must never be null");
        return certificateChain;
    }

    @Override
    public @NotNull Optional<X509Certificate> getClientCertificate() {
        return Optional.ofNullable(certificate);
    }

    @Override
    public @NotNull Optional<X509Certificate[]> getClientCertificateChain() {
        return Optional.ofNullable(certificateChain);
    }

    @Override
    public @NotNull String getCipherSuite() {
        return cipherSuite;
    }

    @Override
    public @NotNull String getProtocol() {
        return protocol;
    }

    @Override
    public @NotNull Optional<String> getHostname() {
        return Optional.ofNullable(hostname);
    }
}
