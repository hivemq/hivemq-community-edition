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

package com.hivemq.extensions.client;

import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.ClientContext;
import com.hivemq.extension.sdk.api.interceptor.Interceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishInboundInterceptor;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.executor.task.AbstractOutput;

import java.util.List;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class ClientContextPluginImpl extends AbstractOutput implements ClientContext {

    @NotNull
    private final IsolatedPluginClassloader pluginClassloader;

    @NotNull
    private final ClientContextImpl clientContext;

    public ClientContextPluginImpl(@NotNull final IsolatedPluginClassloader pluginClassloader,
                                   @NotNull final ClientContextImpl clientContext) {
        this.pluginClassloader = pluginClassloader;
        this.clientContext = clientContext;
    }

    @Override
    public void addPublishInboundInterceptor(@NotNull final PublishInboundInterceptor interceptor) {
        clientContext.addInterceptor(interceptor);
    }

    @Override
    public void removePublishInboundInterceptor(@NotNull final PublishInboundInterceptor interceptor) {
        clientContext.removeInterceptor(interceptor);
    }

    @NotNull
    @Override
    public List<Interceptor> getAllInterceptors() {
        return clientContext.getAllInterceptorsForPlugin(pluginClassloader);
    }

    @NotNull
    @Override
    public List<PublishInboundInterceptor> getPublishInboundInterceptors() {
        return clientContext.getPublishInboundInterceptorsForPlugin(pluginClassloader);
    }


    @NotNull
    @Override
    public ModifiableDefaultPermissions getDefaultPermissions() {
        return clientContext.getDefaultPermissions();
    }
}
