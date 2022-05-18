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
package com.hivemq.persistence.qos;

import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.persistence.local.IncomingMessageFlowLocalPersistence;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.TestMessageUtil;

import static org.mockito.Mockito.verify;

/**
 * @since 4.1.0
 */
@SuppressWarnings("NullabilityAnnotations")
public class IncomingMessageFlowPersistenceImplTest {

    private IncomingMessageFlowPersistenceImpl incomingMessageFlowPersistence;

    @Mock
    private IncomingMessageFlowLocalPersistence incomingMessageFlowLocalPersistence;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        incomingMessageFlowPersistence = new IncomingMessageFlowPersistenceImpl(incomingMessageFlowLocalPersistence);
    }

    @Test
    public void test_delegate_get() {
        incomingMessageFlowPersistence.get("client", 1);
        verify(incomingMessageFlowLocalPersistence).get("client", 1);
    }

    @Test
    public void test_delegate_addOrReplace() {
        final PUBLISH mqtt3Publish = TestMessageUtil.createMqtt3Publish();
        incomingMessageFlowPersistence.addOrReplace("client", 1, mqtt3Publish);
        verify(incomingMessageFlowLocalPersistence).addOrReplace("client", 1, mqtt3Publish);
    }

    @Test
    public void test_delegate_remove() {
        incomingMessageFlowPersistence.remove("client", 1);
        verify(incomingMessageFlowLocalPersistence).remove("client", 1);
    }

    @Test
    public void test_delegate_delete() {
        incomingMessageFlowPersistence.delete("client");
        verify(incomingMessageFlowLocalPersistence).delete("client");
    }

    @Test
    public void test_delegate_closeDB() {
        incomingMessageFlowPersistence.closeDB();
        verify(incomingMessageFlowLocalPersistence).closeDB();
    }
}