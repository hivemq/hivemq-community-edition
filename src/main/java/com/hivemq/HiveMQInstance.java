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
package com.hivemq;

import com.hivemq.bootstrap.HiveMQNettyBootstrap;
import com.hivemq.bootstrap.ListenerStartupInformation;
import com.hivemq.bootstrap.StartupListenerVerifier;
import com.hivemq.embedded.EmbeddedExtension;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.services.admin.AdminService;
import com.hivemq.extensions.ExtensionBootstrap;
import com.hivemq.extensions.services.admin.AdminServiceImpl;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.util.Checkpoints;

import javax.inject.Inject;
import java.util.List;

/**
 * @author Dominik Obermaier
 * @author Florian Limpöck
 */
public class HiveMQInstance {

    private final @NotNull HiveMQNettyBootstrap nettyBootstrap;
    private final @NotNull PublishPayloadPersistence payloadPersistence;
    private final @NotNull ExtensionBootstrap extensionBootstrap;
    private final @NotNull AdminService adminService;

    @Inject
    HiveMQInstance(
            final @NotNull HiveMQNettyBootstrap nettyBootstrap,
            final @NotNull PublishPayloadPersistence payloadPersistence,
            final @NotNull ExtensionBootstrap extensionBootstrap,
            final @NotNull AdminService adminService) {

        this.nettyBootstrap = nettyBootstrap;
        this.payloadPersistence = payloadPersistence;
        this.extensionBootstrap = extensionBootstrap;
        this.adminService = adminService;
    }

    public void start(final @Nullable EmbeddedExtension embeddedExtension) throws Exception {

        payloadPersistence.init();

        extensionBootstrap.startExtensionSystem(embeddedExtension).get();

        final List<ListenerStartupInformation> startupInformation = nettyBootstrap.bootstrapServer().get();
        Checkpoints.checkpoint("listener-started");

        new StartupListenerVerifier(startupInformation).verifyAndPrint();

        ((AdminServiceImpl) adminService).hivemqStarted();
    }
}
