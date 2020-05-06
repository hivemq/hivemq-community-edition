/*
 * Copyright 2019 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hivemq.configuration.info;

import com.google.common.io.Files;
import com.hivemq.HiveMQServer;
import com.hivemq.configuration.EnvironmentVariables;
import com.hivemq.configuration.SystemProperties;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.util.ManifestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * @author Christoph Schäbel
 * @author Silvio Giebl
 */
public class SystemInformationImpl implements SystemInformation {

    private static final Logger log = LoggerFactory.getLogger(SystemInformationImpl.class);
    public static final String DEVELOPMENT_VERSION = "Development Snapshot";

    private @NotNull File homeFolder;
    private @NotNull File configFolder;
    private @NotNull File logFolder;
    private @NotNull File dataFolder;
    private @NotNull File pluginFolder;
    private @NotNull String hivemqVersion;
    private final long runningSince;
    private final boolean embedded;
    private final int processorCount;

    private final boolean usePathOfRunningJar;

    public SystemInformationImpl() {
        this(false);
    }

    public SystemInformationImpl(final boolean usePathOfRunningJar) {
        this(usePathOfRunningJar, false, null, null, null);
    }

    public SystemInformationImpl(
            final boolean usePathOfRunningJar, final boolean embedded, final @Nullable File configFolder,
            final @Nullable File dataFolder, final @Nullable File pluginFolder) {
        this.usePathOfRunningJar = usePathOfRunningJar;
        this.embedded = embedded;
        this.configFolder = configFolder;
        this.dataFolder = dataFolder;
        this.pluginFolder = pluginFolder;


        this.runningSince = System.currentTimeMillis();
        setHiveMQVersion();
        setFolders();
        processorCount = getPhysicalProcessorCount();
    }

    private int getPhysicalProcessorCount() {

        final int runtimeProcessorCount = Runtime.getRuntime().availableProcessors();
        int physicalProcessorCount;
        try {
            physicalProcessorCount = new SystemInfo().getHardware().getProcessor().getPhysicalProcessorCount();
        } catch (final Exception e) {
            log.warn(
                    "No able to determine amount of physical cores, using available amount of cores reported by the JVM as fallback");
            if (log.isTraceEnabled()) {
                log.trace("Original Exception: ", e);
            }
            physicalProcessorCount = runtimeProcessorCount;
        }

        return Math.min(physicalProcessorCount, runtimeProcessorCount);
    }

    private void setFolders() {
        setHomeFolder();
        configFolder = Objects.requireNonNullElse(configFolder, setUpHiveMQFolder(
                SystemProperties.CONFIG_FOLDER, EnvironmentVariables.CONFIG_FOLDER, "conf", false));

        logFolder = setUpHiveMQFolder(SystemProperties.LOG_FOLDER, EnvironmentVariables.LOG_FOLDER, "log", !embedded);
        // Set log folder property for logger-xml-config
        System.setProperty(SystemProperties.LOG_FOLDER, logFolder.getAbsolutePath());

        dataFolder = Objects.requireNonNullElse(dataFolder, setUpHiveMQFolder(
                SystemProperties.DATA_FOLDER, EnvironmentVariables.DATA_FOLDER, "data", true));

        pluginFolder = Objects.requireNonNullElse(pluginFolder, setUpHiveMQFolder(
                SystemProperties.EXTENSIONS_FOLDER, EnvironmentVariables.EXTENSION_FOLDER, "extensions", !embedded));
    }

    private void setHiveMQVersion() {

        hivemqVersion = ManifestUtils.getValueFromManifest(HiveMQServer.class, "HiveMQ-Version");

        if (hivemqVersion == null || hivemqVersion.length() < 1) {
            hivemqVersion = DEVELOPMENT_VERSION;
        }

        log.info("HiveMQ version: {}", hivemqVersion);
    }

    public void setHivemqVersion(final String hivemqVersion) {
        this.hivemqVersion = hivemqVersion;
    }

    @NotNull
    @Override
    public String getHiveMQVersion() {
        return hivemqVersion;
    }

    @NotNull
    @Override
    public File getHiveMQHomeFolder() {
        return homeFolder;
    }

    @NotNull
    @Override
    public File getConfigFolder() {
        return configFolder;
    }

    @NotNull
    @Override
    public File getLogFolder() {
        return logFolder;
    }

    @NotNull
    @Override
    public File getDataFolder() {
        return dataFolder;
    }

    @NotNull
    @Override
    public File getExtensionsFolder() {
        return this.pluginFolder;
    }

    @Override
    public long getRunningSince() {
        return runningSince;
    }

    /**
     * Tries to find a file in the given absolute path or relative to the HiveMQ home folder
     *
     * @param fileLocation the absolute or relative path
     * @return a file
     */
    private File findAbsoluteAndRelative(final String fileLocation) {
        final File file = new File(fileLocation);
        if (file.isAbsolute()) {
            return file;
        } else {
            return new File(getHiveMQHomeFolder(), fileLocation);
        }
    }

    private File setUpHiveMQFolder(
            final String propertyName, final String variableName, final String defaultFolder,
            final boolean createFolderIfNotExists) {
        final String folderName = getSystemPropertyOrEnvironmentVariable(propertyName, variableName);
        final File folder;
        if (folderName != null) {
            folder = findAbsoluteAndRelative(folderName);
        } else {
            folder = findAbsoluteAndRelative(defaultFolder);
        }

        if (createFolderIfNotExists && !folder.exists()) {
            final boolean mkdirsResult = folder.mkdirs();
            if (!mkdirsResult) {
                log.warn("Not able to create folder {}, HiveMQ will behave unexpectedly!", folder);
            }
        }

        return folder;
    }

    /**
     * Returns the value of the system property or environment variable prioritising the system property.
     *
     * @param propertyName the name of the system property
     * @param variableName the name of the environment variable
     * @return value of the system property, if not present value of the environment variable, if not present null
     */
    @Nullable
    private String getSystemPropertyOrEnvironmentVariable(final String propertyName, final String variableName) {
        final String systemProperty = System.getProperty(propertyName);
        if (systemProperty != null) {
            return systemProperty;
        }

        final String environmentVariable = System.getenv().get(variableName);
        return environmentVariable;
    }

    private void setHomeFolder() {
        final String home =
                getSystemPropertyOrEnvironmentVariable(SystemProperties.HIVEMQ_HOME, EnvironmentVariables.HIVEMQ_HOME);

        if (home != null) {
            homeFolder = findAbsoluteAndRelative(home);
            log.info("HiveMQ home directory: {}", homeFolder.getAbsolutePath());

            //setting system property to support the deprecated PathUtils in the SPI
            System.setProperty(SystemProperties.HIVEMQ_HOME, homeFolder.getAbsolutePath());

        } else if (usePathOfRunningJar) {
            usePathOfRunningJarAsHomeFolder();
        } else {
            useTemporaryHomeFolder();
        }
    }

    private void useTemporaryHomeFolder() {
        final File tempDir = Files.createTempDir();
        tempDir.deleteOnExit();
        log.warn(
                "No {} property or {} environment variable was set. Using a temporary directory ({}) HiveMQ will behave unexpectedly!",
                SystemProperties.HIVEMQ_HOME, EnvironmentVariables.HIVEMQ_HOME, tempDir.getAbsolutePath());
        homeFolder = tempDir;
    }

    private void usePathOfRunningJarAsHomeFolder() {
        final File pathOfRunningJar = getPathOfRunningJar();
        if (!embedded) {
            log.warn("No {} property or {} environment variable was set. Using {}",
                    SystemProperties.HIVEMQ_HOME, EnvironmentVariables.HIVEMQ_HOME, pathOfRunningJar.getAbsolutePath());
        }
        homeFolder = pathOfRunningJar;
    }

    private File getPathOfRunningJar() {
        final String decode = URLDecoder.decode(
                HiveMQServer.class.getProtectionDomain().getCodeSource().getLocation().getPath(),
                StandardCharsets.UTF_8);
        final String path = decode.substring(0, decode.lastIndexOf('/') + 1);
        return new File(path);
    }

    @Override
    public int getProcessorCount() {
        return this.processorCount;
    }

    public boolean isEmbedded() {
        return embedded;
    }
}
