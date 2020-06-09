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
package com.hivemq.bootstrap;

import com.hivemq.configuration.service.entity.TcpListener;
import io.netty.channel.ChannelFuture;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertSame;

public class BindInformationTest {


    @Mock
    ChannelFuture future;

    private TcpListener listener;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        listener = new TcpListener(1883, "0.0.0.0");
    }

    @Test
    public void test_bind_information() throws Exception {
        final BindInformation bindInformation = new BindInformation(listener, future);

        assertSame(listener, bindInformation.getListener());
        assertSame(future, bindInformation.getBindFuture());
    }

    @Test(expected = NullPointerException.class)
    public void test_listener_null() throws Exception {
        new BindInformation(null, future);
    }

    @Test(expected = NullPointerException.class)
    public void test_future_null() throws Exception {
        new BindInformation(listener, null);
    }
}