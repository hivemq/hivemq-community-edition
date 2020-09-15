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
package com.hivemq.extensions;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * @author Georg Held
 */

@XmlType(propOrder = {})
@XmlRootElement(name = "hivemq-extension")
public class HiveMQExtensionEntity {

    public static final int DEFAULT_PRIORITY = 0;
    public static final int DEFAULT_START_PRIORITY = 1000;

    @NotNull
    @XmlElement(name = "id", required = true)
    private String id = "";

    @NotNull
    @XmlElement(name = "name", required = true)
    private String name = "";

    @NotNull
    @XmlElement(name = "version", required = true)
    private String version = "";

    @XmlElement(name = "priority", defaultValue = "0")
    private int priority = DEFAULT_PRIORITY;

    @XmlElement(name = "start-priority", defaultValue = "1000")
    private int startPriority = DEFAULT_START_PRIORITY;

    @Nullable
    @XmlElement(name = "author")
    private String author;


    public HiveMQExtensionEntity() {
    }

    public HiveMQExtensionEntity(@NotNull final String id, @NotNull final String name, @NotNull final String version,
                                 final int priority, final int startPriority, @Nullable final String author) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.priority = priority;
        this.startPriority = startPriority;
        this.author = author;
    }

    @NotNull
    public String getId() {
        return this.id;
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    @NotNull
    public String getVersion() {
        return this.version;
    }

    public int getPriority() {
        return this.priority;
    }

    public int getStartPriority() {
        return startPriority;
    }

    @Nullable
    public String getAuthor() {
        return this.author;
    }
}
