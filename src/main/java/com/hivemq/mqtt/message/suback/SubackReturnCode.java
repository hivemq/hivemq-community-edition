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

package com.hivemq.mqtt.message.suback;

/**
 * The return code of a mqtt 3 {@link SUBACK} message.
 *
 * @author Georg Held
 * @author Florian Limpöck
 *
 * @since 4.0.0
 */
public enum SubackReturnCode {
    GRANTED_QOS_0(0x00),
    GRANTED_QOS_1(0x01),
    GRANTED_QOS_2(0x02);

    private final int code;

    SubackReturnCode(final int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static SubackReturnCode valueOf(final int code) {
        for (final SubackReturnCode subackReturnCode : SubackReturnCode.values()) {
            if (subackReturnCode.getCode() == code) {
                return subackReturnCode;
            }
        }
        return null;
    }
}
