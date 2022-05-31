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
import com.hivemq.extension.sdk.api.interceptor.Interceptor;
import com.hivemq.extension.sdk.api.interceptor.pingreq.PingReqInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pingresp.PingRespOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.puback.PubackInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.puback.PubackOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubcomp.PubcompInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubcomp.PubcompOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundInput;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundOutput;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishOutboundOutput;
import com.hivemq.extension.sdk.api.interceptor.pubrec.PubrecInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubrec.PubrecOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubrel.PubrelInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubrel.PubrelOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.subscribe.SubscribeInboundInterceptor;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.packets.general.ModifiableDefaultPermissionsImpl;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import util.TestInterceptorUtil;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @since 4.0.0
 */
public class ClientContextImplTest {

    @Rule
    public final @NotNull TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final @NotNull HiveMQExtensions hiveMQExtensions = mock(HiveMQExtensions.class);

    private final @NotNull ClientContextImpl clientContext =
            new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

    @Test
    public void test_get_interceptors_return_correct_instances() {
        clientContext.addPublishInboundInterceptor((input, output) -> {
        });
        clientContext.addPublishOutboundInterceptor((input, output) -> {
        });
        clientContext.addSubscribeInboundInterceptor((input, output) -> {
        });

        assertEquals(3, clientContext.getAllInterceptors().size());
        assertEquals(1, clientContext.getPublishInboundInterceptors().size());
        assertEquals(1, clientContext.getPublishOutboundInterceptors().size());
        assertEquals(1, clientContext.getSubscribeInboundInterceptors().size());
    }

    @Test
    public void test_add_remove_specific() {
        final PublishInboundInterceptor publishInboundInterceptor = (input, output) -> {
        };
        final PublishOutboundInterceptor publishOutboundInterceptor = (input, output) -> {
        };
        final PingReqInboundInterceptor pingReqInboundInterceptor = (input, output) -> {
        };

        clientContext.addPublishInboundInterceptor(publishInboundInterceptor);
        clientContext.addPublishOutboundInterceptor(publishOutboundInterceptor);
        clientContext.removePublishInboundInterceptor(publishInboundInterceptor);
        clientContext.addPingReqInboundInterceptor(pingReqInboundInterceptor);

        assertEquals(2, clientContext.getAllInterceptors().size());
        assertEquals(0, clientContext.getPublishInboundInterceptors().size());
        assertEquals(1, clientContext.getPublishOutboundInterceptors().size());
        assertEquals(1, clientContext.getPingReqInboundInterceptors().size());

        clientContext.removePublishOutboundInterceptor(publishOutboundInterceptor);

        assertEquals(1, clientContext.getAllInterceptors().size());
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
        clientContext.removeSubscribeInboundInterceptor(subscribeInboundInterceptor);

        assertEquals(1, clientContext.getAllInterceptors().size());
        assertEquals(0, clientContext.getSubscribeInboundInterceptors().size());
        assertEquals(1, clientContext.getPublishInboundInterceptors().size());
    }

    @Test
    public void test_add_remove_puback_interceptors() {
        final PubackOutboundInterceptor pubackOutboundInterceptor = (pubackOutboundInput, pubackOutboundOutput) -> {
        };
        final PubackInboundInterceptor pubackInboundInterceptor = (pubackInboundInput, pubackInboundOutput) -> {
        };

        clientContext.addPubackInboundInterceptor(pubackInboundInterceptor);
        assertEquals(1, clientContext.getPubackInboundInterceptors().size());
        assertSame(pubackInboundInterceptor, clientContext.getPubackInboundInterceptors().get(0));

        clientContext.addPubackOutboundInterceptor(pubackOutboundInterceptor);
        assertEquals(1, clientContext.getPubackOutboundInterceptors().size());
        assertSame(pubackOutboundInterceptor, clientContext.getPubackOutboundInterceptors().get(0));

        assertEquals(2, clientContext.getAllInterceptors().size());

        clientContext.removePubackInboundInterceptor(pubackInboundInterceptor);
        clientContext.removePubackOutboundInterceptor(pubackOutboundInterceptor);

        assertEquals(0, clientContext.getAllInterceptors().size());
    }

    @Test
    public void test_add_remove_pubrec_interceptors() {
        final PubrecOutboundInterceptor pubrecOutboundInterceptor = (pubrecOutboundInput, pubrecOutboundOutput) -> {
        };
        final PubrecInboundInterceptor pubrecInboundInterceptor = (pubrecInboundInput, pubrecInboundOutput) -> {
        };

        clientContext.addPubrecInboundInterceptor(pubrecInboundInterceptor);
        assertEquals(1, clientContext.getPubrecInboundInterceptors().size());
        assertSame(pubrecInboundInterceptor, clientContext.getPubrecInboundInterceptors().get(0));

        clientContext.addPubrecOutboundInterceptor(pubrecOutboundInterceptor);
        assertEquals(1, clientContext.getPubrecOutboundInterceptors().size());
        assertSame(pubrecOutboundInterceptor, clientContext.getPubrecOutboundInterceptors().get(0));

        assertEquals(2, clientContext.getAllInterceptors().size());

        clientContext.removePubrecInboundInterceptor(pubrecInboundInterceptor);
        clientContext.removePubrecOutboundInterceptor(pubrecOutboundInterceptor);

        assertEquals(0, clientContext.getAllInterceptors().size());
    }

    @Test
    public void test_add_remove_pubrel_interceptors() {
        final PubrelOutboundInterceptor pubrelOutboundInterceptor = (pubrelOutboundInput, pubrelOutboundOutput) -> {
        };
        final PubrelInboundInterceptor pubrelInboundInterceptor = (pubrelInboundInput, pubrelInboundOutput) -> {
        };

        clientContext.addPubrelInboundInterceptor(pubrelInboundInterceptor);
        assertEquals(1, clientContext.getPubrelInboundInterceptors().size());
        assertSame(pubrelInboundInterceptor, clientContext.getPubrelInboundInterceptors().get(0));

        clientContext.addPubrelOutboundInterceptor(pubrelOutboundInterceptor);
        assertEquals(1, clientContext.getPubrelOutboundInterceptors().size());
        assertSame(pubrelOutboundInterceptor, clientContext.getPubrelOutboundInterceptors().get(0));

        assertEquals(2, clientContext.getAllInterceptors().size());

        clientContext.removePubrelInboundInterceptor(pubrelInboundInterceptor);
        clientContext.removePubrelOutboundInterceptor(pubrelOutboundInterceptor);

        assertEquals(0, clientContext.getAllInterceptors().size());
    }

    @Test
    public void test_add_remove_pubcomp_interceptors() {
        final PubcompOutboundInterceptor pubcompOutboundInterceptor = (pubcompOutboundInput, pubcompOutboundOutput) -> {
        };
        final PubcompInboundInterceptor pubcompInboundInterceptor = (pubcompInboundInput, pubcompInboundOutput) -> {
        };

        clientContext.addPubcompInboundInterceptor(pubcompInboundInterceptor);
        assertEquals(1, clientContext.getPubcompInboundInterceptors().size());
        assertSame(pubcompInboundInterceptor, clientContext.getPubcompInboundInterceptors().get(0));

        clientContext.addPubcompOutboundInterceptor(pubcompOutboundInterceptor);
        assertEquals(1, clientContext.getPubcompOutboundInterceptors().size());
        assertSame(pubcompOutboundInterceptor, clientContext.getPubcompOutboundInterceptors().get(0));

        assertEquals(2, clientContext.getAllInterceptors().size());

        clientContext.removePubcompInboundInterceptor(pubcompInboundInterceptor);
        clientContext.removePubcompOutboundInterceptor(pubcompOutboundInterceptor);

        assertEquals(0, clientContext.getAllInterceptors().size());
    }

    @Test
    public void test_add_remove_specific_pingreq_pingresp() {
        final PingReqInboundInterceptor pingReqInboundInterceptor = ((input, output) -> {
        });
        final PingRespOutboundInterceptor pingRespOutboundInterceptor = ((input, output) -> {
        });

        clientContext.addPingReqInboundInterceptor(pingReqInboundInterceptor);
        clientContext.addPingRespOutboundInterceptor(pingRespOutboundInterceptor);

        assertEquals(1, clientContext.getPingReqInboundInterceptors().size());
        assertEquals(1, clientContext.getPingRespOutboundInterceptors().size());

        clientContext.removePingReqInboundInterceptor(pingReqInboundInterceptor);
        clientContext.removePingRespOutboundInterceptor(pingRespOutboundInterceptor);

        assertEquals(0, clientContext.getPingReqInboundInterceptors().size());
        assertEquals(0, clientContext.getPingRespOutboundInterceptors().size());
    }

    @Test
    public void multiple_interceptor_implementation() {
        final MultipleInterceptors multipleInterceptors = new MultipleInterceptors();

        clientContext.addPublishInboundInterceptor(multipleInterceptors);

        assertEquals(1, clientContext.getPublishInboundInterceptors().size());
        assertEquals(0, clientContext.getPublishOutboundInterceptors().size());

        clientContext.addPublishOutboundInterceptor(multipleInterceptors);

        assertEquals(1, clientContext.getPublishInboundInterceptors().size());
        assertEquals(1, clientContext.getPublishOutboundInterceptors().size());

        clientContext.removePublishInboundInterceptor(multipleInterceptors);

        assertEquals(0, clientContext.getPublishInboundInterceptors().size());
        assertEquals(1, clientContext.getPublishOutboundInterceptors().size());

        clientContext.removePublishOutboundInterceptor(multipleInterceptors);

        assertEquals(0, clientContext.getPublishInboundInterceptors().size());
        assertEquals(0, clientContext.getPublishOutboundInterceptors().size());
    }

    private static class MultipleInterceptors implements PublishInboundInterceptor, PublishOutboundInterceptor {

        @Override
        public void onInboundPublish(
                final @NotNull PublishInboundInput publishInboundInput,
                final @NotNull PublishInboundOutput publishInboundOutput) {
        }

        @Override
        public void onOutboundPublish(
                final @NotNull PublishOutboundInput publishOutboundInput,
                final @NotNull PublishOutboundOutput publishOutboundOutput) {
        }
    }

    @Test
    public void priority() throws Exception {
        final PublishInboundInterceptor interceptor1 =
                TestInterceptorUtil.getIsolatedInterceptor(TestPublishInboundInterceptor.class, temporaryFolder);
        final PublishInboundInterceptor interceptor2 =
                TestInterceptorUtil.getIsolatedInterceptor(TestPublishInboundInterceptor.class, temporaryFolder);
        final List<? extends PublishInboundInterceptor> interceptors3And4 =
                TestInterceptorUtil.getIsolatedInterceptors(List.of(TestPublishInboundInterceptor.class,
                        TestPublishInboundInterceptor.class), temporaryFolder);
        final PublishInboundInterceptor interceptor3 = interceptors3And4.get(0);
        final PublishInboundInterceptor interceptor4 = interceptors3And4.get(1);

        final HiveMQExtension extension1 = mock(HiveMQExtension.class);
        final HiveMQExtension extension2 = mock(HiveMQExtension.class);
        final HiveMQExtension extension3 = mock(HiveMQExtension.class);
        when(hiveMQExtensions.getExtensionForClassloader(interceptor1.getClass().getClassLoader())).thenReturn(
                extension1);
        when(hiveMQExtensions.getExtensionForClassloader(interceptor2.getClass().getClassLoader())).thenReturn(
                extension2);
        when(hiveMQExtensions.getExtensionForClassloader(interceptor3.getClass().getClassLoader())).thenReturn(
                extension3);
        when(extension1.getPriority()).thenReturn(1);
        when(extension2.getPriority()).thenReturn(2);
        when(extension3.getPriority()).thenReturn(3);

        clientContext.addPublishInboundInterceptor(interceptor1);
        clientContext.addPublishInboundInterceptor(interceptor3);
        clientContext.addPublishInboundInterceptor(interceptor2);
        clientContext.addPublishInboundInterceptor(interceptor4);

        final List<PublishInboundInterceptor> list = clientContext.getPublishInboundInterceptors();
        assertEquals(4, list.size());
        assertSame(interceptor3, list.get(0));
        assertSame(interceptor4, list.get(1));
        assertSame(interceptor2, list.get(2));
        assertSame(interceptor1, list.get(3));

        final List<Interceptor> all = clientContext.getAllInterceptors();
        assertEquals(4, all.size());
        assertSame(interceptor3, all.get(0));
        assertSame(interceptor4, all.get(1));
        assertSame(interceptor2, all.get(2));
        assertSame(interceptor1, all.get(3));
    }

    public static class TestPublishInboundInterceptor implements PublishInboundInterceptor {

        @Override
        public void onInboundPublish(
                final @NotNull PublishInboundInput publishInboundInput,
                final @NotNull PublishInboundOutput publishInboundOutput) {
        }
    }
}
