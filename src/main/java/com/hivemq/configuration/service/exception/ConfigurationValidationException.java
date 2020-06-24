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
package com.hivemq.configuration.service.exception;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * This exception indicates that a configuration was no successful
 *
 * @author Dominik Obermaier
 * @since 3.0
 */
public class ConfigurationValidationException extends RuntimeException {

    private final List<ValidationError> validationErrors;

    public ConfigurationValidationException(final List<ValidationError> validationErrors) {
        this.validationErrors = ImmutableList.copyOf(validationErrors);
    }

    /**
     * @return a list of all Validation errors
     */
    public List<ValidationError> getValidationErrors() {
        return validationErrors;
    }
}
