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
package com.hivemq.persistence.local.xodus;

import com.google.common.annotations.VisibleForTesting;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import jetbrains.exodus.ExodusException;
import jetbrains.exodus.env.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A instance of this class tries to close a Xodus environment even if transactions are running at the moment.
 * <p>
 * It retries until the store was closed successfully or the maximum tries were reached.
 * <p>
 * This class is <b>not</b> thread safe.
 *
 * @author Dominik Obermaier
 */
public class EnvironmentCloser {

    private static final Logger log = LoggerFactory.getLogger(EnvironmentCloser.class);


    private final String name;
    private final Environment environment;
    private final int maxTries;
    private final int retryInterval;

    private int tryNo = 0;

    /**
     * @param name          the name of the environment. Used for logging.
     * @param environment   the environment to close
     * @param maxTries      the maximum tries to close the environment
     * @param retryInterval the retryInterval in milliseconds
     * @throws NullPointerException     if the name or the environment is <code>null</code>
     * @throws IllegalArgumentException if the maxTries or retryinterval is smaller than 1
     */
    public EnvironmentCloser(@NotNull final String name, @NotNull final Environment environment,
                             final int maxTries, final int retryInterval) {

        checkNotNull(name, "Name must not be null");
        checkNotNull(environment, "Environment must not be null");
        checkArgument(maxTries > 0, "maxTries must be higher than 0. %s was provided", maxTries);
        checkArgument(retryInterval > 0, "retryInterval must be higher than 0. %s was provided", retryInterval);

        this.name = name;
        this.environment = environment;
        this.maxTries = maxTries;
        this.retryInterval = retryInterval;
    }

    /**
     * Closes the environment. Retries until the maximum number of retries was hit.
     * <p>
     * All exceptions are rethrown.
     *
     * @return true if the close was successful, false otherwise
     */
    public boolean close() {
        try {
            if (!environment.isOpen()) {
                log.warn("Tried to close store {} although it is already closed", name);
                return false;
            }
            if (tryNo < maxTries) {
                environment.close();
                return true;
            } else {
                log.error("Could not close store {} after {} tries.", name, tryNo);
                return false;
            }
        } catch (final ExodusException e) {
            return retry(e);
        }
    }

    /**
     * Retries if the ExodusException is retryable or rethrows the Exception
     *
     * @param e the exception
     */
    private boolean retry(final ExodusException e) {
        if ("Finish all transactions before closing database environment".equals(e.getMessage())) {
            //This exception means we still have pending transactions
            tryNo += 1;
            log.debug("Could not close {}, transactions still aren't finished yet. Retrying again in {}ms (Retry {} of {})", name, retryInterval, tryNo, maxTries);
            try {
                Thread.sleep(retryInterval);
                //Let's call ourselves recursively!
                return close();
            } catch (final InterruptedException interruptedException) {
                log.debug("Interrupted Exception when trying to close {}", name, interruptedException);
                Thread.currentThread().interrupt();
                return false;
            }
        } else {
            //Not the exception we wanted, rethrow
            throw e;
        }
    }

    @VisibleForTesting
    int getTryNo() {
        return tryNo;
    }
}