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

package com.hivemq.extensions.client;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extension.sdk.api.interceptor.Interceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.subscribe.SubscribeInboundInterceptor;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedExtensionClassloader;
import com.hivemq.extensions.packets.general.ModifiableDefaultPermissionsImpl;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import util.TestInterceptorUtil;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 * @since 4.0.0
 */
public class ClientContextPluginImplTest {

    @Rule
    public final @NotNull TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final @NotNull ServerInformation serverInformation = mock(ServerInformation.class);

    @Test
    public void test_add_and_get_only_interceptors_from_my_class_loader() throws Exception {
        final List<Interceptor> interceptorList = TestInterceptorUtil.getIsolatedInterceptors(temporaryFolder);
        final List<Interceptor> anotherInterceptorList = TestInterceptorUtil.getIsolatedInterceptors(temporaryFolder);

        final ClientContextImpl clientContext =
                new ClientContextImpl(new HiveMQExtensions(serverInformation), new ModifiableDefaultPermissionsImpl());

        final IsolatedExtensionClassloader classloader =
                (IsolatedExtensionClassloader) interceptorList.get(0).getClass().getClassLoader();
        final IsolatedExtensionClassloader anotherClassLoader =
                (IsolatedExtensionClassloader) anotherInterceptorList.get(0).getClass().getClassLoader();

        assertNotNull(classloader);
        assertNotNull(anotherClassLoader);

        final ClientContextPluginImpl contextPlugin1 = new ClientContextPluginImpl(classloader, clientContext);
        final ClientContextPluginImpl contextPlugin2 = new ClientContextPluginImpl(anotherClassLoader, clientContext);

        addInterceptors(interceptorList, contextPlugin1);

        addInterceptors(anotherInterceptorList, contextPlugin2);

        assertEquals(2, contextPlugin1.getAllInterceptors().size());
        assertEquals(1, contextPlugin1.getPublishInboundInterceptors().size());
        assertEquals(1, contextPlugin1.getSubscribeInboundInterceptors().size());

        assertEquals(2, contextPlugin2.getAllInterceptors().size());
        assertEquals(1, contextPlugin2.getPublishInboundInterceptors().size());
        assertEquals(1, contextPlugin2.getSubscribeInboundInterceptors().size());

        for (final Interceptor interceptor : contextPlugin1.getAllInterceptors()) {
            assertFalse(contextPlugin2.getAllInterceptors().contains(interceptor));
        }

        for (final PublishInboundInterceptor interceptor : contextPlugin1.getPublishInboundInterceptors()) {
            assertFalse(contextPlugin2.getPublishInboundInterceptors().contains(interceptor));
        }

        for (final SubscribeInboundInterceptor interceptor : contextPlugin1.getSubscribeInboundInterceptors()) {
            assertFalse(contextPlugin2.getSubscribeInboundInterceptors().contains(interceptor));
        }
    }

    @Test
    public void test_add_and_remove_and_get_only_interceptors_from_my_class_loader() throws Exception {
        final List<Interceptor> interceptorList = TestInterceptorUtil.getIsolatedInterceptors(temporaryFolder);
        final List<Interceptor> anotherInterceptorList = TestInterceptorUtil.getIsolatedInterceptors(temporaryFolder);

        final ClientContextImpl clientContext =
                new ClientContextImpl(new HiveMQExtensions(serverInformation), new ModifiableDefaultPermissionsImpl());

        final IsolatedExtensionClassloader classloader =
                (IsolatedExtensionClassloader) interceptorList.get(0).getClass().getClassLoader();
        final IsolatedExtensionClassloader anotherClassLoader =
                (IsolatedExtensionClassloader) anotherInterceptorList.get(0).getClass().getClassLoader();

        assertNotNull(classloader);
        assertNotNull(anotherClassLoader);

        final ClientContextPluginImpl contextPlugin1 = new ClientContextPluginImpl(classloader, clientContext);
        final ClientContextPluginImpl contextPlugin2 = new ClientContextPluginImpl(anotherClassLoader, clientContext);

        addInterceptors(interceptorList, contextPlugin1);

        addInterceptors(anotherInterceptorList, contextPlugin2);

        removeInterceptors(interceptorList, contextPlugin1);

        assertEquals(0, contextPlugin1.getAllInterceptors().size());
        assertEquals(0, contextPlugin1.getPublishInboundInterceptors().size());
        assertEquals(0, contextPlugin1.getSubscribeInboundInterceptors().size());

        assertEquals(2, contextPlugin2.getAllInterceptors().size());
        assertEquals(1, contextPlugin2.getPublishInboundInterceptors().size());
        assertEquals(1, contextPlugin2.getSubscribeInboundInterceptors().size());
    }

    private void addInterceptors(
            final List<Interceptor> interceptorList, @NotNull final ClientContextPluginImpl contextPlugin) {
        for (final Interceptor interceptor : interceptorList) {
            if (interceptor instanceof PublishInboundInterceptor) {
                contextPlugin.addPublishInboundInterceptor((PublishInboundInterceptor) interceptor);
            }
            if (interceptor instanceof SubscribeInboundInterceptor) {
                contextPlugin.addSubscribeInboundInterceptor((SubscribeInboundInterceptor) interceptor);
            }
        }
    }

    private void removeInterceptors(
            final List<Interceptor> interceptorList, @NotNull final ClientContextPluginImpl contextPlugin) {
        for (final Interceptor interceptor : interceptorList) {
            if (interceptor instanceof PublishInboundInterceptor) {
                contextPlugin.removePublishInboundInterceptor((PublishInboundInterceptor) interceptor);
            }
            if (interceptor instanceof SubscribeInboundInterceptor) {
                contextPlugin.removeSubscribeInboundInterceptor((SubscribeInboundInterceptor) interceptor);
            }
        }
    }
}
