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

package com.hivemq.configuration.entity.listener.tls;

import com.google.common.io.Files;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.configuration.SystemProperties;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;

/**
 * @author Georg Held
 */
@XmlRootElement(name = "keystore")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class KeystoreEntity {

    @XmlElement(name = "path", required = true)
    private @NotNull String path = "";

    @XmlElement(name = "password", required = true)
    private @NotNull String password = "";

    @XmlElement(name = "private-key-password", required = true)
    private @NotNull String privateKeyPassword = "";

    public @NotNull String getPath() {
        return path;
    }

    public @NotNull String getPassword() {
        return password;
    }

    public @NotNull String getPrivateKeyPassword() {
        return privateKeyPassword;
    }

    /*
     * This is a JAXB callback, don't touch the signature!
     */
    @SuppressWarnings({"UnusedParameters", "unused"})
    void afterUnmarshal(final @NotNull Unmarshaller unmarshaller, final @NotNull Object parent) {
        path = findAbsoluteAndRelative(path).getAbsolutePath();
    }

    /**
     * Tries to find a file in the given absolute path or
     * relative to the HiveMQ home folder
     *
     * @param fileLocation the absolute or relative path
     * @return a file
     */
    private @NotNull File findAbsoluteAndRelative(final @NotNull String fileLocation) {
        final File file = new File(fileLocation);
        if (file.isAbsolute()) {
            return file;
        } else {
            return new File(getHiveMQHomeFolder(), fileLocation);
        }
    }

    private @NotNull File getHiveMQHomeFolder() {

        final String homeFolder = System.getProperty(SystemProperties.HIVEMQ_HOME);

        if (homeFolder == null) {
            return Files.createTempDir();
        }
        return new File(homeFolder);
    }

}
