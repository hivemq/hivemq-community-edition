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

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A listener which allows to listen to MQTT traffic over websockets.
 * <p>
 * Use the builder if you want to create a new websocket listener.
 *
 * @author Dominik Obermaier
 * @author Christoph Schaebel
 * @since 3.0
 */
@Immutable
public class WebsocketListener implements Listener {

    private final @NotNull Integer port;

    private final @NotNull String bindAddress;

    private final @NotNull String path;

    private final @NotNull Boolean allowExtensions;

    private final @NotNull List<String> subprotocols;

    private final @NotNull String name;

    protected WebsocketListener(
            final int port,
            final String bindAddress,
            final String path,
            final boolean allowExtensions,
            final List<String> subprotocols,
            final String name) {
        this.port = port;
        this.bindAddress = bindAddress;
        this.path = path;
        this.allowExtensions = allowExtensions;
        this.subprotocols = subprotocols;
        this.name = name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPort() {
        return port;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getBindAddress() {
        return bindAddress;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String readableName() {
        return "Websocket Listener";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull String getName() {
        return name;
    }

    /**
     * @return the path of the websocket
     */
    public String getPath() {
        return path;
    }

    /**
     * @return if websocket extensions are allowed or not
     */
    public Boolean getAllowExtensions() {
        return allowExtensions;
    }

    /**
     * @return a list of all supported subprotocols
     */
    public List<String> getSubprotocols() {
        return subprotocols;
    }

    /**
     * A builder which allows to conveniently build a listener object with a fluent API
     */
    public static class Builder {

        protected Integer port;
        protected String bindAddress;
        protected String path = "";
        protected String name;
        protected boolean allowExtensions = false;
        protected List<String> subprotocols = new ArrayList<>();

        public Builder() {
            //Add default subprotocol which is required by the MQTT spec
            subprotocols.add("mqtt");
        }

        /**
         * Sets the port of the websocket listener
         *
         * @param port the port
         * @return the Builder
         */
        @NotNull
        public Builder port(final int port) {
            this.port = port;
            return this;
        }

        /**
         * Sets the bind address of the websocket listener
         *
         * @param bindAddress the bind address
         * @return the Builder
         */
        @NotNull
        public Builder bindAddress(final @NotNull String bindAddress) {
            checkNotNull(bindAddress);
            this.bindAddress = bindAddress;
            return this;
        }

        /**
         * Sets the websocket path of the websocket listener
         *
         * @param path the path
         * @return the Builder
         */
        @NotNull
        public Builder path(final @NotNull String path) {
            checkNotNull(path);
            this.path = path;
            return this;
        }

        /**
         * Sets the name of the websocket listener
         *
         * @param name the name
         * @return the Builder
         */
        @NotNull
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
        @NotNull
        public Builder allowExtensions(final boolean allowExtensions) {
            this.allowExtensions = allowExtensions;
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
        @NotNull
        public Builder setSubprotocols(final @NotNull List<String> subprotocols) {
            checkNotNull(subprotocols);
            this.subprotocols = ImmutableList.copyOf(subprotocols);
            return this;
        }

        /**
         * Creates the Websocket Listener
         *
         * @return the Websocket Listener
         */
        @NotNull
        public WebsocketListener build() throws IllegalStateException {
            if (port == null) {
                throw new IllegalStateException("The port for a Websocket listener was not set.");
            }

            if (bindAddress == null) {
                throw new IllegalStateException("The bind address for a Websocket listener was not set.");
            }

            if (name == null) {
                name = "websocket-listener-" + port;
            }

            return new WebsocketListener(port, bindAddress, path, allowExtensions, subprotocols, name);
        }
    }
}
