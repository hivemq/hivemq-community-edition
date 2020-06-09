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
package com.hivemq.configuration.service;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public interface SecurityConfigurationService {


    /**
     * Default values
     */
    boolean ALLOW_SERVER_ASSIGNED_CLIENT_ID_DEFAULT = true;
    boolean VALIDATE_UTF_8_DEFAULT = true;
    boolean PAYLOAD_FORMAT_VALIDATION_DEFAULT = false;
    boolean ALLOW_REQUEST_PROBLEM_INFORMATION_DEFAULT = true;

    /**
     * @return true if server may assign a client id when clients connect with zero length client id else false.
     */
    boolean allowServerAssignedClientId();

    /**
     * @return true if topic and client id should be validated to be UTF-8 well formed else false.
     */
    boolean validateUTF8();

    /**
     * @return true if any payload should be validated by payload format indicator else false.
     */
    boolean payloadFormatValidation();

    /**
     * @return true if the request problem information should be allowed else false.
     */
    boolean allowRequestProblemInformation();

    void setValidateUTF8(final boolean validateUTF8);

    void setPayloadFormatValidation(final boolean payloadFormatValidation);

    void setAllowServerAssignedClientId(final boolean allowServerAssignedClientId);

    void setAllowRequestProblemInformation(final boolean allowRequestProblemInformation);
}
