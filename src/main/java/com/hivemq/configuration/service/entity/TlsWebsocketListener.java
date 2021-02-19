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
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A listener which allows to listen to MQTT traffic over secure websockets with TLS.
 * <p>
 * Use the builder if you want to create a new TLS websocket listener.
 *
 * @author Dominik Obermaier
 * @author Christoph Schaebel
 * @since 3.0
 */
@Immutable
public class TlsWebsocketListener extends WebsocketListener implements TlsListener {

    private final Tls tls;

    TlsWebsocketListener(
            final int port,
            final @NotNull String bindAddress,
            final @NotNull String path,
            final @NotNull Boolean allowExtensions,
            final @NotNull List<String> subprotocols,
            final @NotNull Tls tls,
            final @NotNull String name) {
        super(port, bindAddress, path, allowExtensions, subprotocols, name);
        this.tls = tls;
    }

    /**
     * @return the TLS configuration
     */
    @NotNull
    public Tls getTls() {
        return tls;
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public String readableName() {
        return "Websocket Listener with TLS";
    }

    /**
     * A builder which allows to conveniently build a listener object with a fluent API
     */
    public static class Builder extends WebsocketListener.Builder {

        private @Nullable Tls tls;

        /**
         * Sets the TLS configuration of the TLS Websocket listener
         *
         * @param tls the TLS configuration
         * @return the Builder
         */
        @NotNull
        public Builder tls(final @NotNull Tls tls) {
            checkNotNull(tls);
            this.tls = tls;
            return this;
        }

        /**
         * Sets the port of the TLS websocket listener
         *
         * @param port the port
         * @return the Builder
         */
        @NotNull
        @Override
        public Builder port(final int port) {
            super.port(port);
            return this;
        }

        /**
         * Sets the bind address of the TLS websocket listener
         *
         * @param bindAddress the bind address
         * @return the Builder
         */
        @NotNull
        @Override
        public Builder bindAddress(final @NotNull String bindAddress) {
            super.bindAddress(bindAddress);
            return this;
        }

        /**
         * Sets the websocket path of the TLS websocket listener
         *
         * @param path the path
         * @return the Builder
         */
        @NotNull
        @Override
        public Builder path(final @NotNull String path) {
            super.path(path);
            return this;
        }

        /**
         * Sets the name of the websocket listener
         *
         * @param name the name
         * @return the Builder
         */
        @NotNull
        @Override
        public Builder name(final @NotNull String name) {
            checkNotNull(name);
            this.name = name;
            return this;
        }

        /**
         * Sets if websocket extensions should be allowed or not
         *
         * @param allowExtensions if websocket extensions should be allowed or not
         * @return the Builder
         */
        @Override
        @NotNull
        public Builder allowExtensions(final boolean allowExtensions) {
            super.allowExtensions(allowExtensions);
            return this;
        }

        /**
         * Sets a list of subprotocols the websocket listener should support.
         * <p>
         * Typically you should use 'mqtt' and/or 'mqttv3.1
         *
         * @param subprotocols a list of websocket subprotocols
         * @return the Builder
         */
        @Override
        @NotNull
        public Builder setSubprotocols(final @NotNull List<String> subprotocols) {
            super.setSubprotocols(subprotocols);
            return this;
        }

        /**
         * Creates the TLS Websocket Listener
         *
         * @return the TLS Websocket Listener
         */
        @Override
        @NotNull
        public TlsWebsocketListener build() {
            //For validation purposes
            super.build();
            if (tls == null) {
                throw new IllegalStateException("The TLS settings for a TLS Websocket listener was not set.");
            }

            if (name == null) {
                name = "tls-websocket-listener-" + port;
            }

            return new TlsWebsocketListener(port, bindAddress, path, allowExtensions, subprotocols, tls, name);
        }

    }
}
