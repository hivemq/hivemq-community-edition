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
package com.hivemq.extension.sdk.api.auth.parameter;

/**
 * An instance of this interface is provided by the {@link SimpleAuthOutput} and can be used to configure client specific parameters an restrictions.
 *
 * @author Lukas Brandl
 * @since 4.2.0
 */
public interface ModifiableClientSettings {

    /**
     * Set the receive maximum of the client to the given value.
     * The new value overwrites the receive maximum that the client provided via the CONNECT message.
     *
     * @throws IllegalArgumentException if the value less than 1 or more than 65535
     * @param receiveMaximum to be used for the client.
     * @since 4.2.0
     */
    void setClientReceiveMaximum(int receiveMaximum);

    /**
     * @return the value that will be used as receive maximum for the client.
     * @since 4.2.0
     */
    int getReceiveMaximum();
}
