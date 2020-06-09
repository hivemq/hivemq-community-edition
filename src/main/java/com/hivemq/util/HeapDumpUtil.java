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

import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;

/**
 * @author Lukas Brandl
 */
public class HeapDumpUtil {

    private static final String HOTSPOT_BEAN_NAME = "com.sun.management:type=HotSpotDiagnostic";
    private static final String FILE_NAME = "heap.bin";

    private static boolean enabled = false;

    public static boolean enabled() {
        return enabled;
    }

    public static void enable() {
        HeapDumpUtil.enabled = true;
    }

    public static void disable() {
        HeapDumpUtil.enabled = false;
    }

    public static void dumpHeapIfEnabled() {
        if (enabled) {
            dumpHeap();
        }
    }

    private static void dumpHeap() {
        try {
            final Class clazz = Class.forName("com.sun.management.HotSpotDiagnosticMXBean");
            final Method m = clazz.getMethod("dumpHeap", String.class, boolean.class);
            final Object hotspotMBean = getHotspotMBean();
            m.invoke(hotspotMBean, FILE_NAME, true);
        } catch (final Exception ignore) {
        }
    }

    private static Object getHotspotMBean() throws Exception {
        final Class clazz = Class.forName("com.sun.management.HotSpotDiagnosticMXBean");
        final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        final Object bean =
                ManagementFactory.newPlatformMXBeanProxy(server,
                        HOTSPOT_BEAN_NAME, clazz);
        return bean;
    }
}
