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
package com.hivemq.diagnostic.data;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Map;
import java.util.Set;

class SystemPropertyInformation extends AbstractInformation {

    public String getSystemPropertyInformation() {

        final StringBuilder systemPropertyBuilder = new StringBuilder();
        final RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();

        final Map<String, String> systemProperties = runtimeBean.getSystemProperties();
        final Set<String> keys = systemProperties.keySet();
        for (final String key : keys) {

            final String value = systemProperties.get(key);
            addInformation(systemPropertyBuilder, key, value);
        }

        return systemPropertyBuilder.toString();
    }
}
