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
package com.hivemq.configuration.service.entity;

import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The TLS configuration
 *
 * @author Dominik Obermaier
 * @author Christoph Schäbel
 * @since 3.0
 */
@Immutable
public class Tls {

    private final @NotNull String keystorePath;
    private final @NotNull String keystorePassword;
    private final @NotNull String keystoreType;
    private final @NotNull String privateKeyPassword;
    private final @Nullable String truststorePath;
    private final @Nullable String truststorePassword;
    private final @Nullable String truststoreType;
    private final int handshakeTimeout;
    private final @NotNull ClientAuthMode clientAuthMode;
    private final @NotNull List<String> protocols;
    private final @NotNull List<String> cipherSuites;
    private final @Nullable Boolean preferServerCipherSuites;

    /**
     * Creates a new TLS configuration
     *
     * @param keystorePath             the path to the keystore
     * @param keystorePassword         the password for the keystore
     * @param keystoreType             the keystore type. When in doubt, use <b>JKS</b>
     * @param privateKeyPassword       the password to the private key
     * @param truststorePath           the path to the truststore
     * @param truststorePassword       the password for the truststore
     * @param truststoreType           the truststore type. When in doubt, use <b>JKS</b>
     * @param handshakeTimeout         the TLS handshake timeout
     * @param clientAuthMode           the client authentication mode
     * @param protocols                the supported protocols. <code>null</code> means that all enabled protocols by
     *                                 the JVM are enabled
     * @param cipherSuites             the supported cipher suites. <code>null</code> means that all enabled cipher
     *                                 suites by the JVM are enabled
     * @param preferServerCipherSuites if the server cipher suites are preferred over the client cipher suites
     * @since 3.3
     */
    protected Tls(final @NotNull String keystorePath,
                  final @NotNull String keystorePassword,
                  final @NotNull String keystoreType,
                  final @NotNull String privateKeyPassword,
                  final @Nullable String truststorePath,
                  final @Nullable String truststorePassword,
                  final @Nullable String truststoreType,
                  final int handshakeTimeout,
                  final @NotNull ClientAuthMode clientAuthMode,
                  final @NotNull List<String> protocols,
                  final @NotNull List<String> cipherSuites,
                  final @Nullable Boolean preferServerCipherSuites) {

        checkNotNull(clientAuthMode, "clientAuthMode must not be null");
        checkNotNull(protocols, "protocols must not be null");
        checkNotNull(cipherSuites, "cipher suites must not be null");
        this.keystorePath = keystorePath;
        this.keystorePassword = keystorePassword;
        this.keystoreType = keystoreType;
        this.privateKeyPassword = privateKeyPassword;
        this.truststorePath = truststorePath;
        this.truststorePassword = truststorePassword;
        this.truststoreType = truststoreType;
        this.handshakeTimeout = handshakeTimeout;
        this.clientAuthMode = clientAuthMode;
        this.protocols = protocols;
        this.cipherSuites = cipherSuites;
        this.preferServerCipherSuites = preferServerCipherSuites;
    }

    /**
     * @return the keystore path
     */
    public @NotNull String getKeystorePath() {
        return keystorePath;
    }

    /**
     * @return the keystore password
     */
    public @NotNull String getKeystorePassword() {
        return keystorePassword;
    }

    /**
     * @return the keystore type
     */
    public @NotNull String getKeystoreType() {
        return keystoreType;
    }

    /**
     * @return the password of the private key
     */
    public @NotNull String getPrivateKeyPassword() {
        return privateKeyPassword;
    }

    /**
     * @return the truststore path
     */
    public @Nullable String getTruststorePath() {
        return truststorePath;
    }

    /**
     * @return the truststore password
     */
    public @Nullable String getTruststorePassword() {
        return truststorePassword;
    }

    /**
     * @return the truststore type
     */
    public @Nullable String getTruststoreType() {
        return truststoreType;
    }

    /**
     * @return the TLS handshake timeout
     */
    public int getHandshakeTimeout() {
        return handshakeTimeout;
    }

    /**
     * @return the client authentication mode
     */
    public @NotNull ClientAuthMode getClientAuthMode() {
        return clientAuthMode;
    }

    /**
     * @return the enabled TLS protocols
     */
    public @NotNull List<String> getProtocols() {
        return protocols;
    }

    /**
     * @return the enabled cipher suites
     */
    public @NotNull List<String> getCipherSuites() {
        return cipherSuites;
    }

    /**
     * @return if the server cipher suites should be preferred
     */
    public @Nullable Boolean isPreferServerCipherSuites() {
        return preferServerCipherSuites;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Tls tls = (Tls) o;

        if (!keystorePath.equals(tls.keystorePath)) return false;
        if (!keystorePassword.equals(tls.keystorePassword)) return false;
        if (!keystoreType.equals(tls.keystoreType)) return false;
        if (!privateKeyPassword.equals(tls.privateKeyPassword)) return false;
        if (truststorePath != null ? !truststorePath.equals(tls.truststorePath) : tls.truststorePath != null)
            return false;
        if (truststorePassword != null ? !truststorePassword.equals(tls.truststorePassword) : tls.truststorePassword != null)
            return false;
        if (truststoreType != null ? !truststoreType.equals(tls.truststoreType) : tls.truststoreType != null)
            return false;
        if (handshakeTimeout != tls.handshakeTimeout)
            return false;
        if (clientAuthMode != tls.clientAuthMode) return false;
        if (!protocols.equals(tls.protocols)) return false;
        if (preferServerCipherSuites != null ? !preferServerCipherSuites.equals(tls.preferServerCipherSuites) : tls.preferServerCipherSuites != null)
            return false;
        return cipherSuites.equals(tls.cipherSuites);
    }

    @Override
    public int hashCode() {
        int result = keystorePath.hashCode();
        result = 31 * result + keystorePassword.hashCode();
        result = 31 * result + keystoreType.hashCode();
        result = 31 * result + privateKeyPassword.hashCode();
        result = 31 * result + (truststorePath != null ? truststorePath.hashCode() : 0);
        result = 31 * result + (truststorePassword != null ? truststorePassword.hashCode() : 0);
        result = 31 * result + (truststoreType != null ? truststoreType.hashCode() : 0);
        result = 31 * result + handshakeTimeout;
        result = 31 * result + clientAuthMode.hashCode();
        result = 31 * result + protocols.hashCode();
        result = 31 * result + cipherSuites.hashCode();
        result = 31 * result + (preferServerCipherSuites != null ? preferServerCipherSuites.hashCode() : 0);
        return result;
    }

    /**
     * The X509 client certificate authentication mode.
     */
    public enum ClientAuthMode {
        /**
         * Clients are not allowed to send X509 client certificates
         */
        NONE("none"),
        /**
         * Clients can send X509 client certificates but they're not required to do so
         */
        OPTIONAL("optional"),
        /**
         * Clients must send X509 client certificates
         */
        REQUIRED("required");

        private final @NotNull String clientAuthMode;

        ClientAuthMode(final @NotNull String clientAuthMode) {
            this.clientAuthMode = clientAuthMode;
        }

        @Override
        public @NotNull String toString() {
            return clientAuthMode;
        }
    }

    /**
     * A builder which allows to conveniently build a tls object with a fluent API
     */
    public static class Builder {

        private @Nullable String keystorePath;
        private @Nullable String keystorePassword;
        private @Nullable String keystoreType;
        private @Nullable String privateKeyPassword;
        private @Nullable String truststorePath;
        private @Nullable String truststorePassword;
        private @Nullable String truststoreType;
        private int handshakeTimeout;
        private @Nullable ClientAuthMode clientAuthMode;
        private @Nullable List<String> protocols;
        private @Nullable List<String> cipherSuites;
        private @Nullable Boolean preferServerCipherSuites;

        public @NotNull Builder withKeystorePath(final @NotNull String keystorePath) {
            this.keystorePath = keystorePath;
            return this;
        }

        public @NotNull Builder withKeystorePassword(final @NotNull String keystorePassword) {
            this.keystorePassword = keystorePassword;
            return this;
        }

        public @NotNull Builder withKeystoreType(final @NotNull String keystoreType) {
            this.keystoreType = keystoreType;
            return this;
        }

        public @NotNull Builder withPrivateKeyPassword(final @NotNull String privateKeyPassword) {
            this.privateKeyPassword = privateKeyPassword;
            return this;
        }

        public @NotNull Builder withTruststorePath(final @Nullable String truststorePath) {
            this.truststorePath = truststorePath;
            return this;
        }

        public @NotNull Builder withTruststorePassword(final @Nullable String truststorePassword) {
            this.truststorePassword = truststorePassword;
            return this;
        }

        public @NotNull Builder withTruststoreType(final @Nullable String truststoreType) {
            this.truststoreType = truststoreType;
            return this;
        }

        public @NotNull Builder withHandshakeTimeout(final int handshakeTimeout) {
            this.handshakeTimeout = handshakeTimeout;
            return this;
        }

        public @NotNull Builder withClientAuthMode(final @NotNull ClientAuthMode clientAuthMode) {
            this.clientAuthMode = clientAuthMode;
            return this;
        }

        public @NotNull Builder withProtocols(final @NotNull List<String> protocols) {
            this.protocols = protocols;
            return this;
        }

        public @NotNull Builder withCipherSuites(final @NotNull List<String> cipherSuites) {
            this.cipherSuites = cipherSuites;
            return this;
        }

        public @NotNull Builder withPreferServerCipherSuites(final @Nullable Boolean preferServerCipherSuites) {
            this.preferServerCipherSuites = preferServerCipherSuites;
            return this;
        }

        public @NotNull Tls build() {
            checkNotNull(keystorePath, "keystorePath must not be null");
            checkNotNull(keystorePassword, "keystorePassword must not be null");
            checkNotNull(keystoreType, "keystoreType must not be null");
            checkNotNull(privateKeyPassword, "privateKeyPassword must not be null");
            checkNotNull(clientAuthMode, "clientAuthMode must not be null");
            checkNotNull(protocols, "protocols must not be null");
            checkNotNull(cipherSuites, "cipher suites must not be null");

            return new Tls(keystorePath,
                    keystorePassword,
                    keystoreType,
                    privateKeyPassword,
                    truststorePath,
                    truststorePassword,
                    truststoreType,
                    handshakeTimeout,
                    clientAuthMode,
                    protocols,
                    cipherSuites,
                    preferServerCipherSuites) {
            };
        }
    }
}
