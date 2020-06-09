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
package com.hivemq.extensions.services;

import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.extension.sdk.api.services.exception.RateLimitExceededException;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.hivemq.configuration.service.InternalConfigurations.PLUGIN_SERVICE_RATE_LIMIT;

/**
 * @author Lukas Brandl
 */
@LazySingleton
public class PluginServiceRateLimitService {

    public static final RateLimitExceededException RATE_LIMIT_EXCEEDED_EXCEPTION = new RateLimitExceededException();

    private final AtomicInteger counter = new AtomicInteger(0);
    private final AtomicInteger reserveCounter = new AtomicInteger(0);
    private final AtomicLong startTime = new AtomicLong(0);
    private final AtomicLong resetTime = new AtomicLong(0);

    private final int rateLimit;

    public PluginServiceRateLimitService() {
        rateLimit = PLUGIN_SERVICE_RATE_LIMIT.get();
    }

    static {
        RATE_LIMIT_EXCEEDED_EXCEPTION.setStackTrace(new StackTraceElement[0]);
    }

    public boolean rateLimitExceeded() {

        if (rateLimit <= 0) {
            return false;
        }
        final long currentTime = System.currentTimeMillis();
        final long rateTimer = startTime.get();
        if (currentTime - rateTimer >= 1000) {
            // If an other thread sets a new timestamp that's ok as well.
            if (startTime.compareAndSet(rateTimer, currentTime)) {
                counter.set(1); // This call is the first

                //reset reserve after 10 seconds
                final long resetTime = this.resetTime.get();
                if (currentTime - resetTime > 10000) {

                    // If an other thread sets a new timestamp that's ok as well.
                    if (startTime.compareAndSet(resetTime, currentTime)) {
                        reserveCounter.set(1);
                    }
                }

                //first call in this second window
                return false;
            }
        }

        final boolean exceeded = counter.incrementAndGet() > rateLimit;
        if (exceeded) {
            // if counter is exceeded try if there is still a reserve. This allows to handle short bursts even if the second limit is breached.
            return reserveCounter.incrementAndGet() > rateLimit;
        }
        return false;
    }
}
