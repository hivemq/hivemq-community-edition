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
package com.hivemq.security.auth;

import com.hivemq.configuration.service.entity.Listener;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A client token which represents the session
 * information of a connected MQTT client
 */
public class ClientToken implements ClientCredentialsData {

    private final boolean bridge;
    private final String clientId;

    private final Optional<String> username;

    private final Optional<byte[]> password;

    private final Optional<SslClientCertificate> certificate;

    private boolean isAuthenticated;

    private boolean isAnonymous = true;

    private final Optional<InetAddress> inetAddress;

    private final Optional<Listener> listener;

    private final Optional<Long> disconnectTimestamp;

    public ClientToken(@NotNull final String clientId,
                       @Nullable final String username,
                       @Nullable final byte[] password,
                       @Nullable final SslClientCertificate certificate,
                       final boolean isBridge,
                       @Nullable final InetAddress inetAddress,
                       @Nullable final Listener listener) {
        this(clientId, username, password, certificate, isBridge, inetAddress, listener, Optional.empty());
    }

    public ClientToken(@NotNull final String clientId,
                       @Nullable final String username,
                       @Nullable final byte[] password,
                       @Nullable final SslClientCertificate certificate,
                       final boolean isBridge,
                       @Nullable final InetAddress inetAddress,
                       @Nullable final Listener listener,
                       @Nullable final Optional<Long> disconnectTimestamp) {

        this.clientId = checkNotNull(clientId, "client identifier must not be null");
        this.username = Optional.ofNullable(username);
        this.password = Optional.ofNullable(password);
        this.certificate = Optional.ofNullable(certificate);
        this.inetAddress = Optional.ofNullable(inetAddress);
        bridge = isBridge;
        this.listener = Optional.ofNullable(listener);
        this.disconnectTimestamp = disconnectTimestamp;
    }


    public Optional<SslClientCertificate> getCertificate() {
        return certificate;
    }

    @Override
    public boolean isAnonymous() {
        return isAnonymous;
    }

    @Override
    public boolean isBridge() {
        return bridge;
    }

    @Override
    public Optional<InetAddress> getInetAddress() {
        return inetAddress;
    }

    @Override
    public Optional<Listener> getListener() {
        return listener;
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    @Override
    public Optional<String> getPassword() {
        return password.map((input) -> new String(input, StandardCharsets.UTF_8));
    }

    @Override
    public Optional<byte[]> getPasswordBytes() {
        return password;
    }

    @Override
    public Optional<String> getUsername() {
        return username;
    }

    @Override
    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    public void setAuthenticated(final boolean authenticated) {
        isAuthenticated = authenticated;
        isAnonymous = !authenticated;
    }

    @Override
    public Optional<Long> getDisconnectTimestamp() {
        return disconnectTimestamp;
    }

    @Override
    public String toString() {
        return "ClientToken{" +
                "bridge=" + bridge +
                ", clientId='" + clientId + '\'' +
                ", username=" + (username.isPresent() ? username.get() : "null") +
                ", password=" + (password.isPresent() ? "********" : "null") +
                ", certificate=" + (certificate.isPresent() ? "present" : "null") +
                ", isAuthenticated=" + isAuthenticated +
                ", isAnonymous=" + isAnonymous +
                ", inetAddress=" + (inetAddress.isPresent() ? inetAddress.get().getHostAddress() : "null") +
                ", listener=" + (listener.isPresent() ? listener.get() : "null") +
                ", disconnectTimestamp=" + (disconnectTimestamp.isPresent() ? disconnectTimestamp.get() : "null") +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final ClientToken that = (ClientToken) o;

        if (bridge != that.bridge) return false;
        if (isAuthenticated != that.isAuthenticated) return false;
        if (isAnonymous != that.isAnonymous) return false;
        if (clientId != null ? !clientId.equals(that.clientId) : that.clientId != null) return false;
        if (listener != null ? !listener.equals(that.listener) : that.listener != null) return false;
        if (username != null ? !username.equals(that.username) : that.username != null) return false;
        if (password.isPresent() ? !that.password.isPresent() : that.password.isPresent()) {
            return false;
        }
        if ((password.isPresent() && that.password.isPresent()) && !Arrays.equals(password.get(), that.password.get())) {
            return false;
        }
        if (certificate != null ? !certificate.equals(that.certificate) : that.certificate != null) return false;
        if (inetAddress != null ? !inetAddress.equals(that.inetAddress) : that.inetAddress != null) return false;
        if (listener.isPresent() ? !that.listener.isPresent() : that.listener.isPresent())
            return false;
        if ((listener.isPresent() && that.listener.isPresent()) && !listener.get().equals(that.listener.get()))
            return false;
        return (!disconnectTimestamp.isPresent() || that.disconnectTimestamp.isPresent()) && (disconnectTimestamp.isPresent() || !that.disconnectTimestamp.isPresent());
    }

    @Override
    public int hashCode() {
        int result = (bridge ? 1 : 0);
        result = 31 * result + (clientId != null ? clientId.hashCode() : 0);
        result = 31 * result + (username.isPresent() ? username.get().hashCode() : 0);
        result = 31 * result + (listener.isPresent() ? listener.get().hashCode() : 0);
        result = 31 * result + (password.isPresent() ? Arrays.hashCode(password.get()) : 0);
        result = 31 * result + (certificate.isPresent() ? certificate.get().hashCode() : 0);
        result = 31 * result + (isAuthenticated ? 1 : 0);
        result = 31 * result + (isAnonymous ? 1 : 0);
        result = 31 * result + (inetAddress.isPresent() ? inetAddress.hashCode() : 0);
        result = 31 * result + (listener.isPresent() ? listener.hashCode() : 0);
        result = 31 * result + (disconnectTimestamp.isPresent() ? disconnectTimestamp.hashCode() : 0);
        return result;
    }

}