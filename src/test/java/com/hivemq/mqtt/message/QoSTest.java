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
package com.hivemq.mqtt.message;

import org.junit.Test;
import util.EnumTestUtil;

import static com.hivemq.mqtt.message.QoS.*;
import static org.junit.Assert.assertEquals;

public class QoSTest {

    @Test
    public void valueOf_whenCreated_thenHasCorrectQosValue() {
        EnumTestUtil.assertAllValueOfWithFallback(QoS.class, QoS::getQosNumber, QoS::valueOf, null);
    }

    @Test
    public void getMinQoS_whenBothQosEqual_thenResultIsEitherOfInputs() {
        final QoS result = QoS.getMinQoS(AT_LEAST_ONCE, AT_LEAST_ONCE);

        assertEquals(AT_LEAST_ONCE, result);
    }

    @Test
    public void getMinQoS_whenTheSecondQosIsSmaller_thenTheSecondQosIsReturned() {
        final QoS result = QoS.getMinQoS(AT_LEAST_ONCE, AT_MOST_ONCE);
        assertEquals(AT_MOST_ONCE, result);

        final QoS result2 = QoS.getMinQoS(EXACTLY_ONCE, AT_LEAST_ONCE);
        assertEquals(AT_LEAST_ONCE, result2);

        final QoS result3 = QoS.getMinQoS(EXACTLY_ONCE, AT_MOST_ONCE);
        assertEquals(AT_MOST_ONCE, result3);
    }

    @Test
    public void getMinQoS_whenTheFirstQosIsSmaller_thenTheFirstQosIsReturned() {
        final QoS result = QoS.getMinQoS(AT_MOST_ONCE, AT_LEAST_ONCE);
        assertEquals(AT_MOST_ONCE, result);

        final QoS result2 = QoS.getMinQoS(AT_LEAST_ONCE, EXACTLY_ONCE);
        assertEquals(AT_LEAST_ONCE, result2);

        final QoS result3 = QoS.getMinQoS(AT_MOST_ONCE, EXACTLY_ONCE);
        assertEquals(AT_MOST_ONCE, result3);
    }
}
