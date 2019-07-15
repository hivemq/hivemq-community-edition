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

import com.hivemq.configuration.info.SystemInformationImpl;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishOutboundInterceptor;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.client.parameter.ServerInformationImpl;
import com.hivemq.extensions.packets.general.ModifiableDefaultPermissionsImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
@SuppressWarnings("NullabilityAnnotations")
public class ClientContextImplTest {

    private ClientContextImpl clientContext;

    @Mock
    private ServerInformation serverInformation;

    @Before
    public void setUp() throws Exception {
        final HiveMQExtensions hiveMQExtensions = new HiveMQExtensions(serverInformation);
        clientContext = new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());
    }

    @Test
    public void test_get_interceptors_return_correct_instances() {

        clientContext.addPublishInboundInterceptor((input, output) -> {
        });
        clientContext.addPublishOutboundInterceptor((input, output) -> {
        });
        
        assertEquals(2, clientContext.getAllInterceptors().size());
        assertEquals(1, clientContext.getPublishInboundInterceptors().size());
        assertEquals(1, clientContext.getPublishOutboundInterceptors().size());
    }

    @Test
    public void test_add_remove_specific() {


        final PublishInboundInterceptor publishInboundInterceptor = (input, output) -> {
        };
        final PublishOutboundInterceptor publishOutboundInterceptor = (input, output) -> {
        };

        clientContext.addPublishInboundInterceptor(publishInboundInterceptor);
        clientContext.addPublishOutboundInterceptor(publishOutboundInterceptor);
        clientContext.removePublishInboundInterceptor(publishInboundInterceptor);

        assertEquals(1, clientContext.getAllInterceptors().size());
        assertEquals(0, clientContext.getPublishInboundInterceptors().size());
        assertEquals(1, clientContext.getPublishOutboundInterceptors().size());

        clientContext.removePublishOutboundInterceptor(publishOutboundInterceptor);

        assertEquals(0, clientContext.getAllInterceptors().size());
        assertEquals(0, clientContext.getPublishInboundInterceptors().size());
        assertEquals(0, clientContext.getPublishOutboundInterceptors().size());
    }
}