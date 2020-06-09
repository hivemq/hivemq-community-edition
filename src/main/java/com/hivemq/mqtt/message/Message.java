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

import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.io.Serializable;

/**
 * @author Dominik Obermaier
 * @author Florian Limpöck
 * @since 1.4
 */
public interface Message extends Serializable {

    @NotNull MessageType getType();

    /**
     * sets the fully encoded length of that message
     *
     * @param length the length to set
     */
    void setEncodedLength(final int length);

    int getEncodedLength();

    /**
     * sets the remaining length of that message
     *
     * @param length the length to set
     */
    void setRemainingLength(final int length);

    int getRemainingLength();

    /**
     * sets the property length of that message
     *
     * @param length the length to set
     */
    void setPropertyLength(final int length);

    int getPropertyLength();

    /**
     * sets the count of omitted properties
     *
     * @param omittedProperties the count to set
     */
    void setOmittedProperties(final int omittedProperties);

    int getOmittedProperties();

}
