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

package com.hivemq.configuration.entity.listener;

import com.hivemq.configuration.entity.listener.tls.ClientAuthenticationModeEntity;
import com.hivemq.configuration.entity.listener.tls.KeystoreEntity;
import com.hivemq.configuration.entity.listener.tls.TruststoreEntity;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Dominik Obermaier
 * @author Florian Limpöck
 */
@XmlRootElement(name = "tls")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class TLSEntity {

    @XmlElementRef
    private @NotNull KeystoreEntity keystoreEntity = new KeystoreEntity();

    @XmlElementRef(required = false)
    private @NotNull TruststoreEntity truststoreEntity = new TruststoreEntity();

    @XmlElement(name = "handshake-timeout", defaultValue = "10000")
    private @NotNull Integer handshakeTimeout = 10000;

    @XmlElement(name = "client-authentication-mode", defaultValue = "NONE")
    private @NotNull ClientAuthenticationModeEntity clientAuthMode = ClientAuthenticationModeEntity.NONE;

    @XmlElementWrapper(name = "protocols")
    @XmlElement(name = "protocol")
    private @NotNull List<String> protocols = new ArrayList<>();

    @XmlElementWrapper(name = "cipher-suites")
    @XmlElement(name = "cipher-suite")
    private @NotNull List<String> cipherSuites = new ArrayList<>();

    @XmlElement(name = "prefer-server-cipher-suites")
    private @Nullable Boolean preferServerCipherSuites = null;

    public @NotNull KeystoreEntity getKeystoreEntity() {
        return keystoreEntity;
    }

    public @NotNull TruststoreEntity getTruststoreEntity() {
        return truststoreEntity;
    }

    public int getHandshakeTimeout() {
        return handshakeTimeout;
    }

    public @NotNull ClientAuthenticationModeEntity getClientAuthMode() {
        return clientAuthMode;
    }

    public @NotNull List<String> getProtocols() {
        return protocols;
    }

    public @NotNull List<String> getCipherSuites() {
        return cipherSuites;
    }

    public @Nullable Boolean isPreferServerCipherSuites() {
        return preferServerCipherSuites;
    }

}