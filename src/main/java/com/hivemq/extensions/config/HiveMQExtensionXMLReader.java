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
package com.hivemq.extensions.config;

import com.hivemq.configuration.service.exception.ValidationError;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensionEntity;
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
public class HiveMQExtensionXMLReader {

    private static final Logger log = LoggerFactory.getLogger(HiveMQExtensionXMLReader.class);

    @NotNull
    public static Optional<HiveMQExtensionEntity> getExtensionEntityFromXML(@NotNull final Path extensionFolder, final boolean logging) {

        final Path extensionXMLPath = extensionFolder.resolve(HiveMQExtension.HIVEMQ_EXTENSION_XML_FILE);
        if (Files.exists(extensionXMLPath) && logging) {
            log.trace("Found hivemq-extension.xml {}", extensionXMLPath);
        }
        try {
            final JAXBContext context = JAXBContext.newInstance(HiveMQExtensionEntity.class);
            final Unmarshaller unmarshaller = context.createUnmarshaller();
            final HiveMQExtensionEntity unmarshal = (HiveMQExtensionEntity) unmarshaller.unmarshal(extensionXMLPath.toFile());

            final Optional<ValidationError> validationError = validateHiveMQExtensionEntity(unmarshal);

            if (validationError.isPresent()) {
                if (logging) {
                    log.warn("Could not parse \"{}\" in {} because of {}. Not loading extension.", HiveMQExtension.HIVEMQ_EXTENSION_XML_FILE, extensionFolder.toString(), validationError.get().getMessage());
                }
                return Optional.empty();
            }

            return Optional.of(unmarshal);
        } catch (final JAXBException e) {
            if (logging) {
                log.warn("Could not parse \"{}\" in {}. Not loading extension.", HiveMQExtension.HIVEMQ_EXTENSION_XML_FILE, extensionFolder.toString(), e);
            }
            return Optional.empty();
        }
    }

    @NotNull
    private static Optional<ValidationError> validateHiveMQExtensionEntity(@Nullable final HiveMQExtensionEntity hiveMQExtensionEntity) {
        final String message = "missing %s";
        if (hiveMQExtensionEntity == null || hiveMQExtensionEntity.getId().isEmpty()) {
            return Optional.of(new ValidationError(message, "<id>"));
        }

        if ( hiveMQExtensionEntity.getName().isEmpty()) {
            return Optional.of(new ValidationError(message, "<name>"));
        }

        if (hiveMQExtensionEntity.getVersion().isEmpty()) {
            return Optional.of(new ValidationError(message, "<version>"));
        }
        return Optional.empty();
    }
}
