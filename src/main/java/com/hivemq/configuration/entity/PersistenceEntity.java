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
package com.hivemq.configuration.entity;

import com.hivemq.extension.sdk.api.annotations.NotNull;

import javax.xml.bind.annotation.*;

/**
 * @author Lukas Brandl
 */
@XmlRootElement(name = "persistence")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class PersistenceEntity {

    @XmlEnum
    @XmlType(name = "mode")
    public enum PersistenceMode {
        @XmlEnumValue("file") FILE,
        @XmlEnumValue("in-memory") IN_MEMORY
    }

    @XmlElement(name = "mode", defaultValue = "file")
    private @NotNull PersistenceEntity.PersistenceMode mode = PersistenceMode.FILE;

    @NotNull
    public PersistenceMode getMode() {
        return mode;
    }
}
