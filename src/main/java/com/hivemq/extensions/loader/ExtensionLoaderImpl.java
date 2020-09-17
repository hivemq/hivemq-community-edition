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

package com.hivemq.extensions.loader;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.reflect.TypeToken;
import com.hivemq.HiveMQServer;
import com.hivemq.annotations.ReadOnly;
import com.hivemq.embedded.EmbeddedExtension;
import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extensions.*;
import com.hivemq.extensions.classloader.IsolatedExtensionClassloader;
import com.hivemq.extensions.config.HiveMQExtensionXMLReader;
import com.hivemq.extensions.exception.ExtensionLoadingException;
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
public class ExtensionLoaderImpl implements ExtensionLoader {

    private static final Logger log = LoggerFactory.getLogger(ExtensionLoaderImpl.class);

    private final @NotNull ClassServiceLoader serviceLoader;
    private final @NotNull HiveMQExtensions hiveMQExtensions;
    private final @NotNull HiveMQExtensionFactory hiveMQExtensionFactory;
    private final @NotNull ExtensionStaticInitializer staticInitializer;

    @Inject
    @VisibleForTesting
    public ExtensionLoaderImpl(
            final @NotNull ClassServiceLoader serviceLoader,
            final @NotNull HiveMQExtensions hiveMQExtensions,
            final @NotNull HiveMQExtensionFactory hiveMQExtensionFactory,
            final @NotNull ExtensionStaticInitializer staticInitializer) {
        this.serviceLoader = serviceLoader;
        this.hiveMQExtensions = hiveMQExtensions;
        this.hiveMQExtensionFactory = hiveMQExtensionFactory;
        this.staticInitializer = staticInitializer;
    }

    @ReadOnly
    @NotNull
    public <T extends ExtensionMain> ImmutableList<HiveMQExtensionEvent> loadExtensions(
            @NotNull final Path extensionFolder, final boolean permissive, @NotNull final Class<T> desiredExtensionClass) {

        checkNotNull(desiredExtensionClass, "extension class must not be null");
        checkNotNull(extensionFolder, "extension folder must not be null");


        try {
            checkArgument(Files.exists(extensionFolder), "%s does not exist", extensionFolder.toAbsolutePath());
            checkArgument(Files.isReadable(extensionFolder), "%s is not readable", extensionFolder.toAbsolutePath());
            checkArgument(Files.isDirectory(extensionFolder), "%s is not a directory", extensionFolder.toAbsolutePath());
        } catch (final @NotNull IllegalArgumentException exception) {
            if (permissive) {
                log.warn("Extension folder could not be used: \"{}\"", exception.getMessage());
                return ImmutableList.of();
            }
            throw exception;
        }

        final ImmutableList.Builder<HiveMQExtensionEvent> extensions = ImmutableList.builder();
        try {
            final Collection<Path> folders = ExtensionUtil.findAllExtensionFolders(extensionFolder);

            for (final Path folder : folders) {
                final HiveMQExtensionEvent hivemqExtension = processSingleExtensionFolder(folder, desiredExtensionClass);
                if (hivemqExtension != null) {
                    extensions.add(hivemqExtension);
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
            @NotNull final Class<T> desiredExtensionClass,
            @NotNull final String extensionId) {

        checkNotNull(desiredExtensionClass, "extension class must not be null");
        checkNotNull(urls, "urls must not be null");

        if (urls.isEmpty()) {
            return Optional.empty();
        }

        final TypeToken<T> type = TypeToken.of(desiredExtensionClass);
        final ImmutableList.Builder<Class<? extends T>> desiredExtensions = ImmutableList.builder();


        /*
         * We are using a Service Loader mechanism similar to the original Java service
         * loader mechanism (e.g read from META-INF/services).
         * We are not using the default JDK Service Loader, though, because it returns direct instances of the
         * desired Extension classes.
         */

        try {
            final ImmutableList.Builder<Class<? extends ExtensionMain>> allImplementations = ImmutableList.builder();

            for (final URL extensionFileUrl : urls) {


                //We are creating an isolated extension classloader for each extension.
                final URL[] classpath = {extensionFileUrl};

                final IsolatedExtensionClassloader extensionClassloader =
                        new IsolatedExtensionClassloader(classpath, getClass().getClassLoader());

                extensionClassloader.loadClassesWithStaticContext();

                if (!initializeStaticContext(extensionId, extensionClassloader)) {
                    return Optional.empty();
                }

                final Iterable<Class<? extends T>> allExtensionModuleStartingPoints =
                        serviceLoader.load(desiredExtensionClass, extensionClassloader);
                if (Iterables.size(allExtensionModuleStartingPoints) > 1) {
                    log.warn(
                            "Extension {} contains more than one implementation of ExtensionMain. The extension will be disabled.",
                            extensionFileUrl.toString());
                    return Optional.empty();
                }
                for (final Class<? extends ExtensionMain> startingPoint : allExtensionModuleStartingPoints) {
                    allImplementations.add(startingPoint);
                }
            }

            for (final Class<? extends ExtensionMain> implementation : allImplementations.build()) {
                if (type.getRawType().isAssignableFrom(implementation)) {
                    @SuppressWarnings("unchecked") final Class<? extends T> extensionClass =
                            (Class<? extends T>) implementation;
                    desiredExtensions.add(extensionClass);
                } else {
                    log.debug("Extension {} is not a {} Extension and will be ignored",
                            implementation.getName(),
                            type.getRawType().getName());
                }
            }
        } catch (final IOException | ClassNotFoundException | SecurityException e) {
            log.error(
                    "An error occurred while searching the implementations for the extension {}. The extension will be disabled. {} : {}",
                    extensionId,
                    e.getClass().getSimpleName(),
                    e.getMessage());
            return Optional.empty();
        }

        final ImmutableList<Class<? extends T>> desired = desiredExtensions.build();

        if (desired.size() == 1) {
            return Optional.of(desired.get(0));
        }

        if (desired.size() == 0) {
            log.warn(
                    "No implementation of the interface ExtensionMain found in the extension with id \"{}\". The extension will be disabled.",
                    extensionId);
            return Optional.empty();
        }
        log.error(
                "More than one implementation of the interface ExtensionMain found in extension with id {}, this interface can only be implemented once. The extension will be disabled.",
                extensionId);
        return Optional.empty();
    }

    @VisibleForTesting
    @Nullable
    public <T extends ExtensionMain> HiveMQExtensionEvent processSingleExtensionFolder(
            @NotNull final Path extensionFolder, @NotNull final Class<T> desiredClass) {

        final Optional<HiveMQExtensionEntity> xmlEntityOptional =
                HiveMQExtensionXMLReader.getExtensionEntityFromXML(extensionFolder, true);
        if (!xmlEntityOptional.isPresent()) {
            return null;
        }

        final HiveMQExtensionEntity xmlEntity = xmlEntityOptional.get();

        final String[] folderContents = extensionFolder.toFile().list();
        if (folderContents == null || folderContents.length < 1) {
            return null;
        }

        final boolean folderEnabled = !extensionFolder.resolve("DISABLED").toFile().exists();

        //ignore, if this extension with this state is already known to HiveMQ
        if (hiveMQExtensions.isHiveMQExtensionKnown(xmlEntity.getId(), extensionFolder, folderEnabled)) {
            return null;
        }

        final boolean extensionEnabled = hiveMQExtensions.isHiveMQExtensionEnabled(xmlEntity.getId());

        //ignore, if folder and extension disabled.
        if (!folderEnabled && !extensionEnabled) {
            return null;
        }

        final String fileName = extensionFolder.getFileName().toString();

        //check for matching directory name and extensionId
        if (!fileName.equals(xmlEntity.getId())) {
            log.warn(
                    "Found extension directory name not matching to id, ignoring extension with id \"{}\" at {}",
                    xmlEntity.getId(),
                    extensionFolder);
            return null;
        }

        //check if folder is disabled
        if (!folderEnabled) {
            //extension is always enabled here
            return new HiveMQExtensionEvent(HiveMQExtensionEvent.Change.DISABLE,
                    xmlEntity.getId(),
                    xmlEntity.getStartPriority(),
                    extensionFolder,
                    false);
        }

        if (hiveMQExtensions.isHiveMQExtensionIDKnown(xmlEntity.getId()) && extensionEnabled) {
            log.warn("An extension with id \"{}\" is already loaded, ignoring extension at {}",
                    xmlEntity.getId(),
                    extensionFolder);
            return null;
        }

        //load the extension
        final HiveMQExtension hiveMQExtension = loadSingleExtension(extensionFolder, xmlEntity, desiredClass);

        if (hiveMQExtension == null) {
            return null;
        }

        hiveMQExtensions.addHiveMQExtension(hiveMQExtension);

        return new HiveMQExtensionEvent(HiveMQExtensionEvent.Change.ENABLE,
                hiveMQExtension.getId(),
                hiveMQExtension.getStartPriority(),
                extensionFolder,
                false);
    }

    @Override
    public @Nullable HiveMQExtensionEvent loadEmbeddedExtension(final @NotNull EmbeddedExtension embeddedExtension) {


        final HiveMQEmbeddedExtensionImpl extension =
                new HiveMQEmbeddedExtensionImpl(embeddedExtension.getId(),
                        embeddedExtension.getVersion(),
                        embeddedExtension.getName(),
                        embeddedExtension.getAuthor(),
                        embeddedExtension.getPriority(),
                        embeddedExtension.getStartPriority(),
                        embeddedExtension.getExtensionMain(),
                        true);

        final HiveMQExtensionEvent hiveMQExtensionEvent =
                new HiveMQExtensionEvent(HiveMQExtensionEvent.Change.ENABLE, embeddedExtension.getId(), embeddedExtension.getStartPriority(), extension.getExtensionFolderPath(), true);
        hiveMQExtensions.addHiveMQExtension(extension);

        final ClassLoader extensionClassloader = extension.getExtensionClassloader();
        if(extensionClassloader == null){
            throw new IllegalStateException("The extensions class loader must not be null at loading stage");
        }

        //need wrapper to load static context and classes.
        final IsolatedExtensionClassloader isolatedExtensionClassloader = new IsolatedExtensionClassloader(extensionClassloader, HiveMQServer.class.getClassLoader());
        isolatedExtensionClassloader.loadClassesWithStaticContext();

        try {
            staticInitializer.initialize(embeddedExtension.getId(), extensionClassloader);
        } catch (final ExtensionLoadingException e) {
            log.warn(
                    "Embedded extension with id \"{}\" cannot be started, the extension will be disabled. reason: {}",
                    embeddedExtension.getId(),
                    e.getMessage());
            log.debug("Original exception", e);
            Exceptions.rethrowError(e);
            return null;
        }

        return hiveMQExtensionEvent;
    }

    @Nullable <T extends ExtensionMain> HiveMQExtension loadSingleExtension(
            @NotNull final Path extensionFolder,
            @NotNull final HiveMQExtensionEntity xmlEntity,
            @NotNull final Class<T> desiredClass) {
        final ImmutableList.Builder<Path> jarPaths = ImmutableList.builder();
        try (final DirectoryStream<Path> stream = Files.newDirectoryStream(extensionFolder)) {
            for (final Path path : stream) {
                if (path.toString().endsWith(".jar")) {
                    log.trace("Found extension jar {}", path.toString());
                    jarPaths.add(path);
                }
            }
        } catch (final IOException e) {
            log.error("Could not read extension folder {}. Original exception:", extensionFolder, e);
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
                ExtensionUtil.disableExtensionFolder(extensionFolder);
            } catch (final IOException e) {
                log.warn("An extension in folder \"" + extensionFolder.toString() + "\" could not be disabled: ", e);
            }
            return null;
        }

        final Class<? extends T> extensionMainClass = classOptional.get();

        final T instance;
        try {
            instance = extensionMainClass.getDeclaredConstructor().newInstance();
        } catch (final NoSuchMethodException nsme) {
            log.warn("Extension {} cannot be loaded. The {} has no constructor without parameters, " +
                            "a no-arg constructor for a ExtensionMain is required by HiveMQ.",
                    extensionFolder.toAbsolutePath().toString(),
                    extensionMainClass);
            return null;
        } catch (final Exception e) {
            log.warn("Extension {} cannot be loaded. The class {} cannot be instantiated, reason: {}",
                    extensionFolder.toAbsolutePath().toString(),
                    extensionMainClass.getCanonicalName(),
                    e.getMessage());
            log.debug("Original exception:", e);
            return null;
        }

        return hiveMQExtensionFactory.createHiveMQExtension(instance, extensionFolder, xmlEntity, true);
    }

    private boolean initializeStaticContext(
            @NotNull final String hiveMQExtensionID, @NotNull final IsolatedExtensionClassloader classloader) {
        try {
            staticInitializer.initialize(hiveMQExtensionID, classloader);
        } catch (final Throwable e) {
            log.warn(
                    "Extension with id \"{}\" cannot be started, the extension will be disabled. reason: {}",
                    hiveMQExtensionID,
                    e.getMessage());
            log.debug("Original exception", e);
            Exceptions.rethrowError(e);
            return false;
        }
        return true;
    }
}
