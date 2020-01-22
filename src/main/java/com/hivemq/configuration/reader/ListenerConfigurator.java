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

package com.hivemq.configuration.reader;

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.configuration.entity.listener.*;
import com.hivemq.configuration.entity.listener.tls.ClientAuthenticationModeEntity;
import com.hivemq.configuration.service.entity.*;
import com.hivemq.configuration.service.impl.listener.ListenerConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ListenerConfigurator {

    private static final Logger log = LoggerFactory.getLogger(ListenerConfigurator.class);

    private static final String JKS = "JKS";

    private final @NotNull ListenerConfigurationService listenerConfigurationService;

    private final @NotNull List<String> chosenNames;

    public ListenerConfigurator(@NotNull final ListenerConfigurationService listenerConfigurationService) {
        this.listenerConfigurationService = listenerConfigurationService;
        this.chosenNames = new ArrayList<>();
    }

    void setListenerConfig(final @NotNull List<ListenerEntity> entities) {
        final ImmutableList<Listener> listeners = convertListenerEntities(entities);

        for (final Listener listener : listeners) {
            listenerConfigurationService.addListener(listener);
        }
    }

    private @NotNull ImmutableList<Listener> convertListenerEntities(final @NotNull List<ListenerEntity> entities) {
        final ImmutableList.Builder<Listener> builder = ImmutableList.builder();

        for (final ListenerEntity entity : entities) {
            final Listener listener = convertListener(entity);
            if (listener != null) {
                builder.add(listener);
            }
        }

        return builder.build();
    }

    @Nullable Listener convertListener(final @NotNull ListenerEntity entity) {
        if (entity instanceof TCPListenerEntity) {
            return convertTcpListener((TCPListenerEntity) entity);

        } else if (entity instanceof WebsocketListenerEntity) {
            return convertWebsocketListener((WebsocketListenerEntity) entity);

        } else if (entity instanceof TlsTCPListenerEntity) {
            return convertTlsTcpListener((TlsTCPListenerEntity) entity);

        } else if (entity instanceof TlsWebsocketListenerEntity) {
            return convertTlsWebsocketListener((TlsWebsocketListenerEntity) entity);
        }
        return null;
    }

    @NotNull TcpListener convertTcpListener(final @NotNull TCPListenerEntity entity) {
        return new TcpListener(entity.getPort(),
                entity.getBindAddress(),
                getName(entity,"tcp-listener-"));
    }

    @NotNull WebsocketListener convertWebsocketListener(final @NotNull WebsocketListenerEntity entity) {
        return new WebsocketListener.Builder()
                .allowExtensions(entity.isAllowExtensions())
                .bindAddress(entity.getBindAddress())
                .path(entity.getPath())
                .port(entity.getPort())
                .setSubprotocols(entity.getSubprotocols())
                .name(getName(entity, "websocket-listener-"))
                .build();
    }

    @NotNull TlsTcpListener convertTlsTcpListener(final @NotNull TlsTCPListenerEntity entity) {
        return new TlsTcpListener(entity.getPort(), entity.getBindAddress(), convertTls(entity.getTls()),
                getName(entity, "tls-tcp-listener-"));
    }

    @NotNull TlsWebsocketListener convertTlsWebsocketListener(final @NotNull TlsWebsocketListenerEntity entity) {
        return new TlsWebsocketListener.Builder()
                .port(entity.getPort())
                .bindAddress(entity.getBindAddress())
                .path(entity.getPath())
                .allowExtensions(entity.isAllowExtensions())
                .tls(convertTls(entity.getTls()))
                .setSubprotocols(entity.getSubprotocols())
                .name(getName(entity, "tls-websocket-listener-"))
                .build();
    }

    @NotNull
    private String getName(final @NotNull ListenerEntity entity, final @NotNull String defaultPrefix) {

        final String chosenName = (entity.getName() == null || entity.getName().trim().isEmpty()) ? defaultPrefix + entity.getPort() : entity.getName();

        if (chosenNames.contains(chosenName)) {

            int count = 1;
            String newName = chosenName + "-" + count++;
            while (chosenNames.contains(newName)) {
                newName = chosenName + "-" + count++;
            }

            log.warn("Name '{}' already in use. Renaming listener with address '{}' and port '{}' to: '{}'", chosenName, entity.getBindAddress(), entity.getPort(), newName);
            chosenNames.add(newName);
            return newName;
        } else {
            chosenNames.add(chosenName);
            return chosenName;
        }

    }

    @NotNull Tls convertTls(final @NotNull TLSEntity entity) {
        return new Tls.Builder().withKeystorePath(entity.getKeystoreEntity().getPath())
                .withKeystoreType(JKS)
                .withKeystorePassword(entity.getKeystoreEntity().getPassword())
                .withPrivateKeyPassword(entity.getKeystoreEntity().getPrivateKeyPassword())

                .withProtocols(entity.getProtocols())

                .withTruststorePath(entity.getTruststoreEntity().getPath())
                .withTruststoreType(JKS)
                .withTruststorePassword(entity.getTruststoreEntity().getPassword())

                .withClientAuthMode(getClientAuthMode(entity.getClientAuthMode()))
                .withCipherSuites(entity.getCipherSuites())

                .withHandshakeTimeout(entity.getHandshakeTimeout())

                .build();
    }

    @NotNull Tls.ClientAuthMode getClientAuthMode(final @NotNull ClientAuthenticationModeEntity entity) {
        switch (entity) {
            case OPTIONAL:
                return Tls.ClientAuthMode.OPTIONAL;
            case REQUIRED:
                return Tls.ClientAuthMode.REQUIRED;
            case NONE:
                return Tls.ClientAuthMode.NONE;
            default:
                //This should never happen
                return Tls.ClientAuthMode.NONE;
        }
    }

}
