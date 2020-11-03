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
package com.hivemq.util;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

public class PhysicalMemoryUtil {
    public static long physicalMemory() {
        final long heap = Runtime.getRuntime().maxMemory();
        try {
            final OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
            if (!(operatingSystemMXBean instanceof com.sun.management.OperatingSystemMXBean)) {
                return (long) (heap * 1.5);
            }
            final long physicalMemory = ((com.sun.management.OperatingSystemMXBean) operatingSystemMXBean).getTotalPhysicalMemorySize();
            if (physicalMemory > 0) {
                return physicalMemory;
            }
            return (long) (heap * 1.5);
        } catch (final Exception e) {
            return (long) (heap * 1.5);
        }
    }
}
