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
package com.hivemq.configuration.entity;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * @author Lukas Brandl
 */
public class StringTrimAdapter extends XmlAdapter<String, String> {

    @Override
    public String unmarshal(final String value) throws Exception {
        if (value == null) {
            return null;
        }
        return value.trim();
    }

    @Override
    public String marshal(final String value) throws Exception {
        if (value == null) {
            return null;
        }
        return value.trim();
    }
}