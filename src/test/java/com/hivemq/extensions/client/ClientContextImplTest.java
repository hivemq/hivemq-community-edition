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

import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extension.sdk.api.interceptor.pingreq.PingReqInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pingresp.PingRespOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.puback.PubackInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.puback.PubackOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubcomp.PubcompInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubcomp.PubcompOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubrec.PubrecInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubrec.PubrecOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubrel.PubrelInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubrel.PubrelOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.subscribe.SubscribeInboundInterceptor;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.packets.general.ModifiableDefaultPermissionsImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

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
        clientContext.addSubscribeInboundInterceptor((input, output) -> {
        });
        clientContext.addInterceptor((PingReqInboundInterceptor) (input, output) -> {
        });
        clientContext.addInterceptor((PingRespOutboundInterceptor) (input, output) -> {
        });

        assertEquals(5, clientContext.getAllInterceptors().size());
        assertEquals(1, clientContext.getPublishInboundInterceptors().size());
        assertEquals(1, clientContext.getPublishOutboundInterceptors().size());
        assertEquals(1, clientContext.getSubscribeInboundInterceptors().size());
        assertEquals(1, clientContext.getPingRequestInboundInterceptors().size());
        assertEquals(1, clientContext.getPingResponseOutboundInterceptors().size());
    }

    @Test
    public void test_add_remove_specific() {


        final PublishInboundInterceptor publishInboundInterceptor = (input, output) -> {
        };
        final PublishOutboundInterceptor publishOutboundInterceptor = (input, output) -> {
        };

        clientContext.addPublishInboundInterceptor(publishInboundInterceptor);
        clientContext.addPublishOutboundInterceptor(publishOutboundInterceptor);
        clientContext.removeInterceptor(publishInboundInterceptor);

        assertEquals(1, clientContext.getAllInterceptors().size());
        assertEquals(0, clientContext.getPublishInboundInterceptors().size());
        assertEquals(1, clientContext.getPublishOutboundInterceptors().size());

        clientContext.removeInterceptor(publishOutboundInterceptor);

        assertEquals(0, clientContext.getAllInterceptors().size());
        assertEquals(0, clientContext.getPublishInboundInterceptors().size());
        assertEquals(0, clientContext.getPublishOutboundInterceptors().size());

    }

    @Test
    public void test_add_remove_specific_subscribe() {


        final SubscribeInboundInterceptor subscribeInboundInterceptor = (input, output) -> {
        };

        clientContext.addPublishInboundInterceptor((input, output) -> {
        });
        clientContext.addSubscribeInboundInterceptor(subscribeInboundInterceptor);
        clientContext.removeInterceptor(subscribeInboundInterceptor);

        assertEquals(1, clientContext.getAllInterceptors().size());
        assertEquals(0, clientContext.getSubscribeInboundInterceptors().size());
        assertEquals(1, clientContext.getPublishInboundInterceptors().size());

    }

    @Test
    public void test_add_remove_puback_interceptors() {
        final PubackOutboundInterceptor pubackOutboundInterceptor = (pubackOutboundInput, pubackOutboundOutput) -> { };

        final PubackInboundInterceptor pubackInboundInterceptor = (pubackInboundInput, pubackInboundOutput) -> { };

        clientContext.addInterceptor(pubackInboundInterceptor);
        assertEquals(1, clientContext.getPubackInboundInterceptors().size());
        assertSame(pubackInboundInterceptor, clientContext.getPubackInboundInterceptors().get(0));

        clientContext.addInterceptor(pubackOutboundInterceptor);
        assertEquals(1, clientContext.getPubackOutboundInterceptors().size());
        assertSame(pubackOutboundInterceptor, clientContext.getPubackOutboundInterceptors().get(0));

        assertEquals(2, clientContext.getAllInterceptors().size());

        clientContext.removeInterceptor(pubackInboundInterceptor);
        clientContext.removeInterceptor(pubackOutboundInterceptor);

        assertEquals(0, clientContext.getAllInterceptors().size());
    }

    @Test
    public void test_add_remove_pubrec_interceptors() {
        final PubrecOutboundInterceptor pubrecOutboundInterceptor = (pubackOutboundInput, pubackOutboundOutput) -> { };

        final PubrecInboundInterceptor pubrecInboundInterceptor = (pubrecInboundInput, pubrecInboundOutput) -> { };

        clientContext.addInterceptor(pubrecInboundInterceptor);
        assertEquals(1, clientContext.getPubrecInboundInterceptors().size());
        assertSame(pubrecInboundInterceptor, clientContext.getPubrecInboundInterceptors().get(0));

        clientContext.addInterceptor(pubrecOutboundInterceptor);
        assertEquals(1, clientContext.getPubrecOutboundInterceptors().size());
        assertSame(pubrecOutboundInterceptor, clientContext.getPubrecOutboundInterceptors().get(0));

        assertEquals(2, clientContext.getAllInterceptors().size());

        clientContext.removeInterceptor(pubrecInboundInterceptor);
        clientContext.removeInterceptor(pubrecOutboundInterceptor);

        assertEquals(0, clientContext.getAllInterceptors().size());
    }

    @Test
    public void test_add_remove_pubrel_interceptors() {
        final PubrelOutboundInterceptor pubrelOutboundInterceptor = (pubrelOutboundInput, pubrelOutboundOutput) -> { };

        final PubrelInboundInterceptor pubrelInboundInterceptor = (pubrelInboundInput, pubrelInboundOutput) -> { };

        clientContext.addInterceptor(pubrelInboundInterceptor);
        assertEquals(1, clientContext.getPubrelInboundInterceptors().size());
        assertSame(pubrelInboundInterceptor, clientContext.getPubrelInboundInterceptors().get(0));

        clientContext.addInterceptor(pubrelOutboundInterceptor);
        assertEquals(1, clientContext.getPubrelOutboundInterceptors().size());
        assertSame(pubrelOutboundInterceptor, clientContext.getPubrelOutboundInterceptors().get(0));

        assertEquals(2, clientContext.getAllInterceptors().size());

        clientContext.removeInterceptor(pubrelInboundInterceptor);
        clientContext.removeInterceptor(pubrelOutboundInterceptor);

        assertEquals(0, clientContext.getAllInterceptors().size());
    }

    @Test
    public void test_add_remove_pubcomp_interceptors() {
        final PubcompOutboundInterceptor pubcompOutboundInterceptor =
                (pubcompOutboundInput, pubcompOutboundOutput) -> { };

        final PubcompInboundInterceptor pubcompInboundInterceptor = (pubcompInboundInput, pubcompInboundOutput) -> { };

        clientContext.addInterceptor(pubcompInboundInterceptor);
        assertEquals(1, clientContext.getPubcompInboundInterceptors().size());
        assertSame(pubcompInboundInterceptor, clientContext.getPubcompInboundInterceptors().get(0));

        clientContext.addInterceptor(pubcompOutboundInterceptor);
        assertEquals(1, clientContext.getPubcompOutboundInterceptors().size());
        assertSame(pubcompOutboundInterceptor, clientContext.getPubcompOutboundInterceptors().get(0));

        assertEquals(2, clientContext.getAllInterceptors().size());

        clientContext.removeInterceptor(pubcompInboundInterceptor);
        clientContext.removeInterceptor(pubcompOutboundInterceptor);

        assertEquals(0, clientContext.getAllInterceptors().size());
    }

    @Test
    public void test_add_remove_specific_pingreq_pingresp() {
        final PingReqInboundInterceptor pingReqInboundInterceptor = ((input, output) -> {

        });
        final PingRespOutboundInterceptor pingRespOutboundInterceptor = ((input, output) -> {

        });

        clientContext.addInterceptor(pingReqInboundInterceptor);
        clientContext.addInterceptor(pingRespOutboundInterceptor);

        assertEquals(1, clientContext.getPingRequestInboundInterceptors().size());
        assertEquals(1, clientContext.getPingResponseOutboundInterceptors().size());

        clientContext.removeInterceptor(pingReqInboundInterceptor);
        clientContext.removeInterceptor(pingRespOutboundInterceptor);

        assertEquals(0, clientContext.getPingRequestInboundInterceptors().size());
        assertEquals(0, clientContext.getPingResponseOutboundInterceptors().size());
    }
}