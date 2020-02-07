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

package com.hivemq.extensions.loader;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.reflect.TypeToken;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.annotations.ReadOnly;
import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extensions.*;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.config.HiveMQPluginXMLReader;
import com.hivemq.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dominik Obermaier
 * @author Georg Held
 * @author Christoph Schäbel
 */
@Singleton
public class PluginLoaderImpl implements PluginLoader {

    private static final Logger log = LoggerFactory.getLogger(PluginLoaderImpl.class);
    @NotNull
    private final ClassServiceLoader serviceLoader;
    @NotNull
    private final HiveMQExtensions hiveMQExtensions;
    @NotNull
    private final HiveMQPluginFactory hiveMQPluginFactory;
    @NotNull
    private final PluginStaticInitializer staticInitializer;

    @Inject
    @VisibleForTesting
    public PluginLoaderImpl(
            @NotNull final ClassServiceLoader serviceLoader,
            @NotNull final HiveMQExtensions hiveMQExtensions,
            @NotNull final HiveMQPluginFactory hiveMQPluginFactory,
            @NotNull final PluginStaticInitializer staticInitializer) {
        this.serviceLoader = serviceLoader;
        this.hiveMQExtensions = hiveMQExtensions;
        this.hiveMQPluginFactory = hiveMQPluginFactory;
        this.staticInitializer = staticInitializer;
    }

    @ReadOnly
    @NotNull
    public <T extends ExtensionMain> ImmutableList<HiveMQPluginEvent> loadPlugins(
            @NotNull final Path pluginFolder,
            @NotNull final Class<T> desiredPluginClass) {

        checkNotNull(desiredPluginClass, "extension class must not be null");
        checkNotNull(pluginFolder, "extension folder must not be null");

        checkArgument(Files.exists(pluginFolder), "%s does not exist", pluginFolder.toAbsolutePath());
        checkArgument(Files.isReadable(pluginFolder), "%s is not readable", pluginFolder.toAbsolutePath());
        checkArgument(Files.isDirectory(pluginFolder), "%s is not a directory", pluginFolder.toAbsolutePath());

        final ImmutableList.Builder<HiveMQPluginEvent> extensions = ImmutableList.builder();
        try {
            final Collection<Path> folders = PluginUtil.findAllPluginFolders(pluginFolder);

            for (final Path folder : folders) {
                final HiveMQPluginEvent hivemqPlugin = processSinglePluginFolder(folder, desiredPluginClass);
                if (hivemqPlugin != null) {
                    extensions.add(hivemqPlugin);
                }
            }
        } catch (final IOException e) {
            log.error("Could not read extensions. Original exception:", e);
        }

        return extensions.build();
    }

    @VisibleForTesting
    @NotNull <T extends ExtensionMain> Optional<Class<? extends T>> loadFromUrls(
            @NotNull final Collection<URL> urls,
            @NotNull final Class<T> desiredPluginClass, @NotNull final String pluginId) {

        checkNotNull(desiredPluginClass, "extension class must not be null");
        checkNotNull(urls, "urls must not be null");

        if (urls.isEmpty()) {
            return Optional.empty();
        }

        final TypeToken<T> type = TypeToken.of(desiredPluginClass);
        final ImmutableList.Builder<Class<? extends T>> desiredPlugins = ImmutableList.builder();


        /*
         * We are using a Service Loader mechanism similar to the original Java service
         * loader mechanism (e.g read from META-INF/services).
         * We are not using the default JDK Service Loader, though, because it returns direct instances of the
         * desired Extension classes.
         */

        try {
            final ImmutableList.Builder<Class<? extends ExtensionMain>> allImplementations = ImmutableList.builder();

            for (final URL pluginFileUrl : urls) {


                //We are creating an isolated extension classloader for each extension.
                final URL[] classpath = {pluginFileUrl};

                final IsolatedPluginClassloader pluginClassloader =
                        new IsolatedPluginClassloader(classpath, getClass().getClassLoader());

                pluginClassloader.loadClassesWithStaticContext();

                if (!initializeStaticContext(pluginId, pluginClassloader)) {
                    return Optional.empty();
                }

                final Iterable<Class<? extends T>> allPluginModuleStartingPoints =
                        serviceLoader.load(desiredPluginClass, pluginClassloader);
                if (Iterables.size(allPluginModuleStartingPoints) > 1) {
                    log.warn(
                            "Extension {} contains more than one implementation of ExtensionMain. The extension will be disabled.",
                            pluginFileUrl.toString());
                    return Optional.empty();
                }
                for (final Class<? extends ExtensionMain> startingPoint : allPluginModuleStartingPoints) {
                    allImplementations.add(startingPoint);
                }
            }

            for (final Class<? extends ExtensionMain> implementation : allImplementations.build()) {
                if (type.getRawType().isAssignableFrom(implementation)) {
                    @SuppressWarnings("unchecked") final Class<? extends T> pluginClass =
                            (Class<? extends T>) implementation;
                    desiredPlugins.add(pluginClass);
                } else {
                    log.debug(
                            "Extension {} is not a {} Extension and will be ignored", implementation.getName(),
                            type.getRawType().getName());
                }
            }
        } catch (final IOException | ClassNotFoundException | SecurityException e) {
            log.error(
                    "An error occurred while searching the implementations for the extension {}. The extension will be disabled. {} : {}",
                    pluginId, e.getClass().getSimpleName(), e.getMessage());
            return Optional.empty();
        }

        final ImmutableList<Class<? extends T>> desired = desiredPlugins.build();

        if (desired.size() == 1) {
            return Optional.of(desired.get(0));
        }

        if (desired.size() == 0) {
            log.warn(
                    "No implementation of the interface ExtensionMain found in the extension with id \"{}\". The extension will be disabled.",
                    pluginId);
            return Optional.empty();
        }
        log.error(
                "More than one implementation of the interface ExtensionMain found in extension with id {}, this interface can only be implemented once. The extension will be disabled.",
                pluginId);
        return Optional.empty();
    }

    @VisibleForTesting
    @Nullable
    public <T extends ExtensionMain> HiveMQPluginEvent processSinglePluginFolder(
            @NotNull final Path pluginFolder, @NotNull final Class<T> desiredClass) {

        final Optional<HiveMQPluginEntity> xmlEntityOptional =
                HiveMQPluginXMLReader.getPluginEntityFromXML(pluginFolder, true);
        if (!xmlEntityOptional.isPresent()) {
            return null;
        }

        final HiveMQPluginEntity xmlEntity = xmlEntityOptional.get();

        final String[] folderContents = pluginFolder.toFile().list();
        if (folderContents == null || folderContents.length < 1) {
            return null;
        }

        final boolean folderEnabled = !pluginFolder.resolve("DISABLED").toFile().exists();

        //ignore, if this extension with this state is already known to HiveMQ
        if (hiveMQExtensions.isHiveMQExtensionKnown(xmlEntity.getId(), pluginFolder, folderEnabled)) {
            return null;
        }

        final boolean pluginEnabled = hiveMQExtensions.isHiveMQExtensionEnabled(xmlEntity.getId());

        //ignore, if folder and plugin disabled.
        if (!folderEnabled && !pluginEnabled) {
            return null;
        }

        final String fileName = pluginFolder.getFileName().toString();

        //check for matching directory name and pluginId
        if (!fileName.equals(xmlEntity.getId())) {
            log.warn(
                    "Found extension directory name not matching to id, ignoring extension with id \"{}\" at {}",
                    xmlEntity.getId(), pluginFolder);
            return null;
        }

        //check if folder is disabled
        if (!folderEnabled) {
            //plugin is always enabled here
            return new HiveMQPluginEvent(HiveMQPluginEvent.Change.DISABLE, xmlEntity.getId(), pluginFolder);
        }

        if (hiveMQExtensions.isHiveMQPluginIDKnown(xmlEntity.getId()) && pluginEnabled) {
            log.warn(
                    "An extension with id \"{}\" is already loaded, ignoring extension at {}", xmlEntity.getId(),
                    pluginFolder);
            return null;
        }

        //load the extension
        final HiveMQExtension hiveMQExtension = loadSinglePlugin(pluginFolder, xmlEntity, desiredClass);

        if (hiveMQExtension == null) {
            return null;
        }

        hiveMQExtensions.addHiveMQPlugin(hiveMQExtension);

        return new HiveMQPluginEvent(HiveMQPluginEvent.Change.ENABLE, hiveMQExtension.getId(), pluginFolder);
    }

    @Nullable <T extends ExtensionMain> HiveMQExtension loadSinglePlugin(
            @NotNull final Path pluginFolder, @NotNull final HiveMQPluginEntity xmlEntity,
            @NotNull final Class<T> desiredClass) {
        final ImmutableList.Builder<Path> jarPaths = ImmutableList.builder();
        try (final DirectoryStream<Path> stream = Files.newDirectoryStream(pluginFolder)) {
            for (final Path path : stream) {
                if (path.toString().endsWith(".jar")) {
                    log.trace("Found extension jar {}", path.toString());
                    jarPaths.add(path);
                }
            }
        } catch (final IOException e) {
            log.error("Could not read extension folder {}. Original exception:", pluginFolder, e);
            return null;
        }

        final ImmutableList.Builder<URL> urls = ImmutableList.builder();
        for (final Path path : jarPaths.build()) {
            try {
                urls.add(path.toUri().toURL());
            } catch (final MalformedURLException e) {
                log.warn("Could not add " + path.toAbsolutePath().toString() +
                        " to the list of files considered for extension discovery");
                log.debug("Original exception:", e);
            }
        }

        final Optional<Class<? extends T>> classOptional = loadFromUrls(urls.build(), desiredClass, xmlEntity.getId());

        if (!classOptional.isPresent()) {
            try {
                PluginUtil.disablePluginFolder(pluginFolder);
            } catch (final IOException e) {
                log.warn("An extension in folder \"" + pluginFolder.toString() + "\" could not be disabled: ", e);
            }
            return null;
        }

        final Class<? extends T> pluginMainClass = classOptional.get();

        final T instance;
        try {
            instance = pluginMainClass.getDeclaredConstructor().newInstance();
        } catch (final NoSuchMethodException nsme) {
            log.warn("Extension {} cannot be loaded. The {} has no constructor without parameters, " +
                            "a no-arg constructor for a ExtensionMain is required by HiveMQ.",
                    pluginFolder.toAbsolutePath().toString(), pluginMainClass);
            return null;
        } catch (final Exception e) {
            log.warn("Extension {} cannot be loaded. The class {} cannot be instantiated, reason: {}",
                    pluginFolder.toAbsolutePath().toString(), desiredClass.getCanonicalName(),
                    pluginMainClass.getCanonicalName(), e.getMessage());
            log.debug("Original exception:", e);
            return null;
        }

        return hiveMQPluginFactory.createHiveMQPlugin(instance, pluginFolder, xmlEntity, true);
    }

    private boolean initializeStaticContext(
            @NotNull final String hiveMQPluginID, @NotNull final IsolatedPluginClassloader classloader) {
        try {
            staticInitializer.initialize(hiveMQPluginID, classloader);
        } catch (final Throwable e) {
            log.warn(
                    "Extension with id \"{}\" cannot be started, the extension will be disabled. reason: {}",
                    hiveMQPluginID, e.getMessage());
            log.debug("Original exception", e);
            Exceptions.rethrowError(e);
            return false;
        }
        return true;
    }
}
