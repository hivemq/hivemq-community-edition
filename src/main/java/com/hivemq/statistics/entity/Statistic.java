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
package com.hivemq.statistics.entity;

/**
 * @author Christoph Schäbel
 */
public class Statistic {

    private String statisticType;
    private String id;
    private String hivemqVersion;
    private long hivemqUptime;
    private Number connectedClients;
    private long officialExtensions;
    private long enterpriseExtensions;
    private long customExtensions;
    private long tcpListeners;
    private long tlsListeners;
    private long wsListeners;
    private long wssListeners;
    private long maxQueue;
    private long maxKeepalive;
    private long sessionExpiry;
    private long messageExpiry;
    private boolean overloadProtection;
    private long connectionThrottling;
    private long bandwithIncoming;
    private String javaVendor;
    private String javaVendorVersion;
    private String javaVersion;
    private String javaVersionDate;
    private String javaVirtualMachineName;
    private String javaRuntimeName;
    private String osManufacturer;
    private String os;
    private String osVersion;
    private long osUptime;
    private long openFileLimit;
    private String systemArchitecture;
    private String cpu;
    private long cpuSockets;
    private long cpuPhysicalCores;
    private long cpuTotalCores;
    private long memorySize;
    private long diskSize;
    private String cloudPlatform;
    private String container;

    public String getStatisticType() {
        return statisticType;
    }

    public void setStatisticType(final String statisticType) {
        this.statisticType = statisticType;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getHivemqVersion() {
        return hivemqVersion;
    }

    public void setHivemqVersion(final String hivemqVersion) {
        this.hivemqVersion = hivemqVersion;
    }

    public long getHivemqUptime() {
        return hivemqUptime;
    }

    public void setHivemqUptime(final long hivemqUptime) {
        this.hivemqUptime = hivemqUptime;
    }

    public Number getConnectedClients() {
        return connectedClients;
    }

    public void setConnectedClients(final Number connectedClients) {
        this.connectedClients = connectedClients;
    }

    public long getOfficialExtensions() {
        return officialExtensions;
    }

    public void setOfficialExtensions(final long officialExtensions) {
        this.officialExtensions = officialExtensions;
    }

    public long getCustomExtensions() {
        return customExtensions;
    }

    public void setCustomExtensions(final long customExtensions) {
        this.customExtensions = customExtensions;
    }

    public long getTcpListeners() {
        return tcpListeners;
    }

    public void setTcpListeners(final long tcpListeners) {
        this.tcpListeners = tcpListeners;
    }

    public long getTlsListeners() {
        return tlsListeners;
    }

    public void setTlsListeners(final long tlsListeners) {
        this.tlsListeners = tlsListeners;
    }

    public long getWsListeners() {
        return wsListeners;
    }

    public void setWsListeners(final long wsListeners) {
        this.wsListeners = wsListeners;
    }

    public long getWssListeners() {
        return wssListeners;
    }

    public void setWssListeners(final long wssListeners) {
        this.wssListeners = wssListeners;
    }

    public long getMaxQueue() {
        return maxQueue;
    }

    public void setMaxQueue(final long maxQueue) {
        this.maxQueue = maxQueue;
    }

    public long getMaxKeepalive() {
        return maxKeepalive;
    }

    public void setMaxKeepalive(final long maxKeepalive) {
        this.maxKeepalive = maxKeepalive;
    }

    public long getSessionExpiry() {
        return sessionExpiry;
    }

    public void setSessionExpiry(final long sessionExpiry) {
        this.sessionExpiry = sessionExpiry;
    }

    public long getMessageExpiry() {
        return messageExpiry;
    }

    public void setMessageExpiry(final long messageExpiry) {
        this.messageExpiry = messageExpiry;
    }

    public long getConnectionThrottling() {
        return connectionThrottling;
    }

    public void setConnectionThrottling(final long connectionThrottling) {
        this.connectionThrottling = connectionThrottling;
    }

    public long getBandwithIncoming() {
        return bandwithIncoming;
    }

    public void setBandwithIncoming(final long bandwithIncoming) {
        this.bandwithIncoming = bandwithIncoming;
    }

    public String getJavaVendor() {
        return javaVendor;
    }

    public void setJavaVendor(final String javaVendor) {
        this.javaVendor = javaVendor;
    }

    public String getJavaVendorVersion() {
        return javaVendorVersion;
    }

    public void setJavaVendorVersion(final String javaVendorVersion) {
        this.javaVendorVersion = javaVendorVersion;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public void setJavaVersion(final String javaVersion) {
        this.javaVersion = javaVersion;
    }

    public String getJavaVersionDate() {
        return javaVersionDate;
    }

    public void setJavaVersionDate(final String javaVersionDate) {
        this.javaVersionDate = javaVersionDate;
    }

    public String getJavaVirtualMachineName() {
        return javaVirtualMachineName;
    }

    public void setJavaVirtualMachineName(final String javaVirtualMachineName) {
        this.javaVirtualMachineName = javaVirtualMachineName;
    }

    public String getJavaRuntimeName() {
        return javaRuntimeName;
    }

    public void setJavaRuntimeName(final String javaRuntimeName) {
        this.javaRuntimeName = javaRuntimeName;
    }

    public String getOsManufacturer() {
        return osManufacturer;
    }

    public void setOsManufacturer(final String osManufacturer) {
        this.osManufacturer = osManufacturer;
    }

    public String getOs() {
        return os;
    }

    public void setOs(final String os) {
        this.os = os;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(final String osVersion) {
        this.osVersion = osVersion;
    }

    public long getOsUptime() {
        return osUptime;
    }

    public void setOsUptime(final long osUptime) {
        this.osUptime = osUptime;
    }

    public long getOpenFileLimit() {
        return openFileLimit;
    }

    public void setOpenFileLimit(final long openFileLimit) {
        this.openFileLimit = openFileLimit;
    }

    public String getSystemArchitecture() {
        return systemArchitecture;
    }

    public void setSystemArchitecture(final String systemArchitecture) {
        this.systemArchitecture = systemArchitecture;
    }

    public String getCpu() {
        return cpu;
    }

    public void setCpu(final String cpu) {
        this.cpu = cpu;
    }

    public long getCpuSockets() {
        return cpuSockets;
    }

    public void setCpuSockets(final long cpuSockets) {
        this.cpuSockets = cpuSockets;
    }

    public long getCpuPhysicalCores() {
        return cpuPhysicalCores;
    }

    public void setCpuPhysicalCores(final long cpuPhysicalCores) {
        this.cpuPhysicalCores = cpuPhysicalCores;
    }

    public long getCpuTotalCores() {
        return cpuTotalCores;
    }

    public void setCpuTotalCores(final long cpuTotalCores) {
        this.cpuTotalCores = cpuTotalCores;
    }

    public long getMemorySize() {
        return memorySize;
    }

    public void setMemorySize(final long memorySize) {
        this.memorySize = memorySize;
    }

    public long getDiskSize() {
        return diskSize;
    }

    public void setDiskSize(final long diskSize) {
        this.diskSize = diskSize;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(final String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public String getContainer() {
        return container;
    }

    public void setContainer(final String container) {
        this.container = container;
    }
}
