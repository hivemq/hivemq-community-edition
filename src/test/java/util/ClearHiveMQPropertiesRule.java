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
package util;

import org.junit.contrib.java.lang.system.ClearSystemProperties;

import static com.hivemq.configuration.SystemProperties.*;

/**
 * * The {@code ClearHiveMQPropertiesRule} rule clears a set of system
 * properties which are set by HiveMQ when the test starts and restores their original values
 * when the test finishes (whether it passes or fails).
 *
 * @author Christoph Schäbel
 */
public class ClearHiveMQPropertiesRule extends ClearSystemProperties {

    public ClearHiveMQPropertiesRule() {
        super(HIVEMQ_HOME, LOG_FOLDER, CONFIG_FOLDER, DATA_FOLDER);
    }

    public void reset() {
        this.after();
    }
}
