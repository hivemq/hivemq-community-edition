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
package com.hivemq.security.auth;

import com.google.common.base.Optional;

/**
 * An extended version of {@link ClientData} which also contains
 * the password the client used in the {@link com.hivemq.mqtt.message.connect.CONNECT}
 * message
 *
 * @author Christian Goetz
 * @since 1.4
 */
public interface ClientCredentialsData extends ClientData {
    /**
     * Returns an {@link Optional} of the clear text password the client used encoded
     * as UTF-8.
     * <p>
     * Consider using the {@link #getPasswordBytes()} method instead, since passwords
     * are not required to be human readable and can be any arbitrary byte array
     *
     * @return the password, if present
     */
    Optional<String> getPassword();


    /**
     * Returns an {@link Optional} of the text password the client used
     *
     * @return the password, if present
     * @since 3.0.0
     */
    Optional<byte[]> getPasswordBytes();
}
