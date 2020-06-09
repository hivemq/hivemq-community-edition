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
package com.hivemq.configuration.service.impl;

import com.hivemq.configuration.service.SecurityConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class SecurityConfigurationServiceImpl implements SecurityConfigurationService {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfigurationServiceImpl.class);

    private final AtomicBoolean allowServerAssignedClientId = new AtomicBoolean(ALLOW_SERVER_ASSIGNED_CLIENT_ID_DEFAULT);
    private final AtomicBoolean validateUTF8 = new AtomicBoolean(VALIDATE_UTF_8_DEFAULT);
    private final AtomicBoolean payloadFormatValidation = new AtomicBoolean(PAYLOAD_FORMAT_VALIDATION_DEFAULT);
    private final AtomicBoolean allowRequestProblemInformation = new AtomicBoolean(ALLOW_REQUEST_PROBLEM_INFORMATION_DEFAULT);

    @Override
    public boolean allowServerAssignedClientId() {
        return allowServerAssignedClientId.get();
    }

    @Override
    public boolean validateUTF8() {
        return validateUTF8.get();
    }

    @Override
    public boolean payloadFormatValidation() {
        return payloadFormatValidation.get();
    }

    @Override
    public boolean allowRequestProblemInformation() {
        return allowRequestProblemInformation.get();
    }

    @Override
    public void setValidateUTF8(final boolean validateUTF8) {
        log.debug("Setting validate UTF-8 to {}", validateUTF8);
        this.validateUTF8.set(validateUTF8);
    }

    @Override
    public void setPayloadFormatValidation(final boolean payloadFormatValidation) {
        log.debug("Setting payload format validation to {}", payloadFormatValidation);
        this.payloadFormatValidation.set(payloadFormatValidation);
    }

    @Override
    public void setAllowServerAssignedClientId(final boolean allowServerAssignedClientId) {
        log.debug("Setting allow server assigned client identifier to {}", allowServerAssignedClientId);
        this.allowServerAssignedClientId.set(allowServerAssignedClientId);
    }

    @Override
    public void setAllowRequestProblemInformation(final boolean allowRequestProblemInformation) {
        log.debug("Setting allow-problem-information to {}", allowRequestProblemInformation);
        this.allowRequestProblemInformation.set(allowRequestProblemInformation);
    }
}
