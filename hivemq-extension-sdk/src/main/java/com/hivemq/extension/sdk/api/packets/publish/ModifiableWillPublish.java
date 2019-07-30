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

package com.hivemq.extension.sdk.api.packets.publish;

import com.hivemq.extension.sdk.api.packets.connect.WillPublishPacket;

/**
 * @author Lukas Brandl
 */
public interface ModifiableWillPublish extends WillPublishPacket, ModifiablePublishPacket {

    /**
     * Sets the will delay.
     *
     * @param willDelay The new will delay for the will publish in seconds.
     * @throws IllegalArgumentException If the delay is less than zero or more than '4294967295'.
     * @since 4.2.0
     */
    void setWillDelay(long willDelay);
}
