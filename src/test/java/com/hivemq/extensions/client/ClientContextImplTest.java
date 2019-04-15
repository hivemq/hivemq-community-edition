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

import com.hivemq.extension.sdk.api.interceptor.publish.PublishInboundInterceptor;
import com.hivemq.extensions.HiveMQPlugins;
import com.hivemq.extensions.packets.general.ModifiableDefaultPermissionsImpl;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
@SuppressWarnings("NullabilityAnnotations")
public class ClientContextImplTest {

    private ClientContextImpl clientContext;

    @Before
    public void setUp() throws Exception {
        final HiveMQPlugins hiveMQPlugins = new HiveMQPlugins();
        clientContext = new ClientContextImpl(hiveMQPlugins, new ModifiableDefaultPermissionsImpl());
    }

    @Test
    public void test_get_interceptors_return_correct_instances() {

        clientContext.addPublishInboundInterceptor((input, output) -> {
        });

        assertEquals(1, clientContext.getAllInterceptors().size());
        assertEquals(1, clientContext.getPublishInboundInterceptors().size());


    }

    @Test
    public void test_add_remove_specific() {


        final PublishInboundInterceptor publishInboundInterceptor = (input, output) -> {
        };

        clientContext.addPublishInboundInterceptor(publishInboundInterceptor);
        clientContext.removePublishInboundInterceptor(publishInboundInterceptor);

        assertEquals(0, clientContext.getAllInterceptors().size());
        assertEquals(0, clientContext.getPublishInboundInterceptors().size());

    }
}