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

import java.nio.file.Path;
import java.util.Objects;

/**
 * @author Georg Held
 */
public class HiveMQExtensionEvent {
    private final Change change;
    private final String extensionId;
    private final int priority;
    private final Path extensionFolder;
    private final boolean embedded;

    public HiveMQExtensionEvent(
            @NotNull final Change change,
            @NotNull final String extensionId,
            final int priority,
            @NotNull final Path extensionFolder,
            final boolean embedded) {
        this.change = change;
        this.extensionId = extensionId;
        this.priority = priority;
        this.extensionFolder = extensionFolder;
        this.embedded = embedded;
    }

    @NotNull
    public Change getChange() {
        return change;
    }

    @NotNull
    public String getExtensionId() {
        return extensionId;
    }

    public int getPriority() {
        return priority;
    }

    @NotNull
    public Path getExtensionFolder() {
        return extensionFolder;
    }

    public boolean isEmbedded() {
        return embedded;
    }

    public enum Change {ENABLE, DISABLE}

    @Override
    public String toString() {
        return "HiveMQExtensionEvent{" +
                "change=" + change +
                ", extensionId='" + extensionId + '\'' +
                ", priority=" + priority +
                ", extensionFolder=" + extensionFolder +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HiveMQExtensionEvent that = (HiveMQExtensionEvent) o;
        return priority == that.priority && change == that.change && Objects.equals(extensionId, that.extensionId) && Objects.equals(extensionFolder, that.extensionFolder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(change, extensionId, priority, extensionFolder);
    }
}
