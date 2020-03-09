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

package com.hivemq.extensions.config;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.configuration.service.exception.ValidationError;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQPluginEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * @author Georg Held
 */
@Singleton
public class HiveMQPluginXMLReader {

    private static final Logger log = LoggerFactory.getLogger(HiveMQPluginXMLReader.class);

    @NotNull
    public static Optional<HiveMQPluginEntity> getPluginEntityFromXML(@NotNull final Path pluginFolder, final boolean logging) {

        final Path pluginXMLPath = pluginFolder.resolve(HiveMQExtension.HIVEMQ_EXTENSION_XML_FILE);
        if (Files.exists(pluginXMLPath) && logging) {
            log.trace("Found hivemq-extension.xml {}", pluginXMLPath);
        }
        try {
            final JAXBContext context = JAXBContext.newInstance(HiveMQPluginEntity.class);
            final Unmarshaller unmarshaller = context.createUnmarshaller();
            final HiveMQPluginEntity unmarshal = (HiveMQPluginEntity) unmarshaller.unmarshal(pluginXMLPath.toFile());

            final Optional<ValidationError> validationError = validateHiveMQPluginEntity(unmarshal);

            if (validationError.isPresent()) {
                if (logging) {
                    log.warn("Could not parse \"{}\" in {} because of {}. Not loading extension.", HiveMQExtension.HIVEMQ_EXTENSION_XML_FILE, pluginFolder.toString(), validationError.get().getMessage());
                }
                return Optional.empty();
            }

            return Optional.of(unmarshal);
        } catch (final JAXBException e) {
            if (logging) {
                log.warn("Could not parse \"{}\" in {}. Not loading extension.", HiveMQExtension.HIVEMQ_EXTENSION_XML_FILE, pluginFolder.toString(), e);
            }
            return Optional.empty();
        }
    }

    @NotNull
    private static Optional<ValidationError> validateHiveMQPluginEntity(@Nullable final HiveMQPluginEntity hiveMQPluginEntity) {
        final String message = "missing %s";
        if (hiveMQPluginEntity == null || hiveMQPluginEntity.getId().isEmpty()) {
            return Optional.of(new ValidationError(message, "<id>"));
        }

        if ( hiveMQPluginEntity.getName().isEmpty()) {
            return Optional.of(new ValidationError(message, "<name>"));
        }

        if (hiveMQPluginEntity.getVersion().isEmpty()) {
            return Optional.of(new ValidationError(message, "<version>"));
        }
        return Optional.empty();
    }
}
