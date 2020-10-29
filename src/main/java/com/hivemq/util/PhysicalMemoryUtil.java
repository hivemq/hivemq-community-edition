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
