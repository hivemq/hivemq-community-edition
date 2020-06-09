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
package com.hivemq.configuration;

import com.hivemq.configuration.service.exception.ValidationError;

import java.util.List;

/**
 * The configuration validator
 *
 * @author Dominik Obermaier
 * @since 3.0
 */
public interface Validator<T> {


    /**
     * Validate a given parameter for a given name.
     *
     * @param parameter The parameter to validate.
     * @param name      The name of the parameter.
     * @return A list of {@link ValidationError}s.
     */
    List<ValidationError> validate(final T parameter, final String name);
}
