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

package com.hivemq.configuration.reader;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.hivemq.configuration.entity.listener.*;
import com.hivemq.configuration.entity.listener.tls.ClientAuthenticationModeEntity;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.service.entity.*;
import com.hivemq.configuration.service.impl.listener.ListenerConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ListenerConfigurator {

    private static final Logger log = LoggerFactory.getLogger(ListenerConfigurator.class);

    private static final String JKS = "JKS";

    private final @NotNull ListenerConfigurationService listenerConfigurationService;
    private final @NotNull SystemInformation systemInformation;

    private final @NotNull List<String> chosenNames;

    public ListenerConfigurator(
            final @NotNull ListenerConfigurationService listenerConfigurationService,
            final @NotNull SystemInformation systemInformation) {
        this.listenerConfigurationService = listenerConfigurationService;
        this.systemInformation = systemInformation;
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
        return new TcpListener(entity.getPort(), entity.getBindAddress(), getName(entity, "tcp-listener-"));
    }

    @NotNull WebsocketListener convertWebsocketListener(final @NotNull WebsocketListenerEntity entity) {
        return new WebsocketListener.Builder().allowExtensions(entity.isAllowExtensions())
                .bindAddress(entity.getBindAddress())
                .path(entity.getPath())
                .port(entity.getPort())
                .setSubprotocols(entity.getSubprotocols())
                .name(getName(entity, "websocket-listener-"))
                .build();
    }

    @NotNull TlsTcpListener convertTlsTcpListener(final @NotNull TlsTCPListenerEntity entity) {
        return new TlsTcpListener(entity.getPort(),
                entity.getBindAddress(),
                convertTls(entity.getTls()),
                getName(entity, "tls-tcp-listener-"));
    }

    @NotNull TlsWebsocketListener convertTlsWebsocketListener(final @NotNull TlsWebsocketListenerEntity entity) {
        return new TlsWebsocketListener.Builder().port(entity.getPort())
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

        final String chosenName =
                (entity.getName() == null || entity.getName().trim().isEmpty()) ? defaultPrefix + entity.getPort() :
                        entity.getName();

        if (chosenNames.contains(chosenName)) {

            int count = 1;
            String newName = chosenName + "-" + count++;
            while (chosenNames.contains(newName)) {
                newName = chosenName + "-" + count++;
            }

            log.warn(
                    "Name '{}' already in use. Renaming listener with address '{}' and port '{}' to: '{}'",
                    chosenName,
                    entity.getBindAddress(),
                    entity.getPort(),
                    newName);
            chosenNames.add(newName);
            return newName;
        } else {
            chosenNames.add(chosenName);
            return chosenName;
        }

    }

    @NotNull Tls convertTls(final @NotNull TLSEntity entity) {

        final String keystorePath = getPathFromEntityPath(entity.getKeystoreEntity().getPath());
        final String truststorePath = getPathFromEntityPath(entity.getTruststoreEntity().getPath());

        Preconditions.checkNotNull(keystorePath, "Keystore path must not be null");

        return new Tls.Builder().withKeystorePath(keystorePath)
                .withKeystoreType(JKS)
                .withKeystorePassword(entity.getKeystoreEntity().getPassword())
                .withPrivateKeyPassword(entity.getKeystoreEntity().getPrivateKeyPassword())

                .withProtocols(entity.getProtocols())

                .withTruststorePath(truststorePath)
                .withTruststoreType(JKS)
                .withTruststorePassword(entity.getTruststoreEntity().getPassword())

                .withClientAuthMode(getClientAuthMode(entity.getClientAuthMode()))
                .withCipherSuites(entity.getCipherSuites())

                .withHandshakeTimeout(entity.getHandshakeTimeout())

                .build();
    }

    /**
     * Tries to find a file if set in the given absolute path or relative to the HiveMQ home folder.
     *
     * @param path the absolute or relative path set in the config entity.
     * @return the absolute path to the file or null if unset.
     */
    @Nullable
    private String getPathFromEntityPath(final @NotNull String path) {
        //blank is default for unused
        if (path.isBlank()) {
            return null;
        } else {
            final File file = new File(path);
            if (file.isAbsolute()) {
                return file.getAbsolutePath();
            } else {
                return new File(systemInformation.getHiveMQHomeFolder(), path).getAbsolutePath();
            }
        }
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
