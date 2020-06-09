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
package com.hivemq.metrics.gauges;

import com.hivemq.persistence.retained.RetainedMessagePersistence;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author Christoph Schäbel
 */
public class RetainedMessagesGaugeTest {

    @Mock
    RetainedMessagePersistence retainedMessagePersistence;

    private RetainedMessagesGauge retainedMessagesGauge;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        retainedMessagesGauge = new RetainedMessagesGauge(retainedMessagePersistence);
    }

    @Test
    public void test_getValue() throws Exception {

        when(retainedMessagePersistence.size()).thenReturn(452l);

        final Long value = retainedMessagesGauge.getValue();

        assertEquals(452l, value.longValue());
    }
}