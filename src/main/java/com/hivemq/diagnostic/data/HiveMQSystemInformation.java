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

import com.sun.management.UnixOperatingSystemMXBean;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

/**
 * @author Lukas Brandl
 */
public class HiveMQSystemInformation extends AbstractInformation {

    public String getSystemInformation() {

        try {
            final OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
            final StringBuilder systemInformationBuilder = new StringBuilder();


            addInformation(systemInformationBuilder, "Available Processors", String.valueOf(operatingSystemMXBean.getAvailableProcessors()));
            addInformation(systemInformationBuilder, "System Load Average", String.valueOf(operatingSystemMXBean.getSystemLoadAverage()));


            //We can gather more information if we have a concrete subclass
            if (operatingSystemMXBean instanceof com.sun.management.OperatingSystemMXBean) {
                final com.sun.management.OperatingSystemMXBean systemMXBean = (com.sun.management.OperatingSystemMXBean) operatingSystemMXBean;
                addInformation(systemInformationBuilder, "Total Swap Space Size", String.valueOf(systemMXBean.getTotalSwapSpaceSize()));
                addInformation(systemInformationBuilder, "Committed Virtual Memory Size", String.valueOf(systemMXBean.getCommittedVirtualMemorySize()));
                addInformation(systemInformationBuilder, "Total Physical Memory Size", String.valueOf(systemMXBean.getCommittedVirtualMemorySize()));
            }

            //We get even more information if we're running on a Linux System
            if (operatingSystemMXBean instanceof UnixOperatingSystemMXBean) {
                final UnixOperatingSystemMXBean systemMXBean = (UnixOperatingSystemMXBean) operatingSystemMXBean;
                addInformation(systemInformationBuilder, "Max File Descriptor Count", String.valueOf(systemMXBean.getMaxFileDescriptorCount()));
                addInformation(systemInformationBuilder, "Open File Descriptor Count", String.valueOf(systemMXBean.getOpenFileDescriptorCount()));
            }
            return systemInformationBuilder.toString();
        } catch (final Exception e) {
            return "Could not get System Information. Exception: " + ExceptionUtils.getStackTrace(e);
        }
    }

}
