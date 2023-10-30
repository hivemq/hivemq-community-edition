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
package com.hivemq.bootstrap;

import com.hivemq.configuration.service.entity.Listener;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This class verifies if the listener startup was successful and gives
 * feedback to the user by logging
 *
 * @author Dominik Obermaier
 */
public class StartupListenerVerifier {

    private static final Logger log = LoggerFactory.getLogger(StartupListenerVerifier.class);
    private final List<ListenerStartupInformation> startupInformation;

    public StartupListenerVerifier(@NotNull final List<ListenerStartupInformation> startupInformation) {

        checkNotNull(startupInformation);
        this.startupInformation = startupInformation;
    }

    /**
     * Verifies that at least one listener was started successfully and prints out information
     * to the user to the log file
     *
     * @throws UnrecoverableException if no listener could be started successfully
     */
    public void verifyAndPrint() throws UnrecoverableException {
        if (startupInformation.isEmpty()) {
            log.error(
                    "No listener was configured. In order to operate properly, HiveMQ needs at least one listener. Shutting down HiveMQ");
            throw new UnrecoverableException(false);
        }

        int successfullyStarted = 0;
        for (final ListenerStartupInformation startupInfo : startupInformation) {
            if (startupInfo.isSuccessful()) {
                log.info(getSuccessfulStartedString(startupInfo));
                successfullyStarted += 1;
            } else {
                log.error(getNotSuccessfulStartedString(startupInfo));
                if (startupInfo.getException().isPresent()) {
                    log.debug("Original Exception:", startupInfo.getException().get());
                }
            }
        }

        if (successfullyStarted < 1) {
            log.error("Could not bind any listener. Stopping HiveMQ.");
            throw new UnrecoverableException(false);
        }
    }

    private @NotNull String getSuccessfulStartedString(final @NotNull ListenerStartupInformation startupInfo) {
        final Listener listener = startupInfo.getListener();
        return String.format("Started %s on address %s and on port %s.",
                listener.readableName(),
                listener.getBindAddress(),
                listener.getPort());
    }

    private @NotNull String getNotSuccessfulStartedString(final @NotNull ListenerStartupInformation startupInfo) {
        final Listener listener = startupInfo.getListener();
        return String.format("Could not start %s on port %s and address %s. Is it already in use?",
                listener.readableName(),
                listener.getPort(),
                listener.getBindAddress());
    }
}
