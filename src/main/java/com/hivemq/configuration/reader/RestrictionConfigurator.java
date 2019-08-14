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

package com.hivemq.configuration.reader;

import com.hivemq.annotations.NotNull;
import com.hivemq.configuration.entity.RestrictionsEntity;
import com.hivemq.configuration.service.RestrictionsConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.hivemq.configuration.service.RestrictionsConfigurationService.*;

public class RestrictionConfigurator {

    private static final Logger log = LoggerFactory.getLogger(RestrictionConfigurator.class);

    private final @NotNull RestrictionsConfigurationService restrictionsConfigurationService;

    public RestrictionConfigurator(@NotNull final RestrictionsConfigurationService restrictionsConfigurationService) {
        this.restrictionsConfigurationService = restrictionsConfigurationService;
    }


    void setRestrictionsConfig(@NotNull final RestrictionsEntity restrictionsEntity) {
        restrictionsConfigurationService.setMaxConnections(restrictionsEntity.getMaxConnections());
        restrictionsConfigurationService.setMaxClientIdLength(restrictionsEntity.getMaxClientIdLength());
        restrictionsConfigurationService.setNoConnectIdleTimeout(restrictionsEntity.getNoConnectIdleTimeout());
        restrictionsConfigurationService.setIncomingLimit(validateIncomingLimit(restrictionsEntity.getIncomingBandwidthThrottling()));
        restrictionsConfigurationService.setMaxTopicLength(restrictionsEntity.getMaxTopicLength());
    }

    private long validateIncomingLimit(final long incomingLimit) {
        if (incomingLimit < INCOMING_BANDWIDTH_THROTTLING_MINIMUM) {
            log.warn("The configured incoming bandwidth throttling ({}) must not be negative. It was set to {} instead.", incomingLimit, INCOMING_BANDWIDTH_THROTTLING_DEFAULT);
            return INCOMING_BANDWIDTH_THROTTLING_DEFAULT;
        }
        return incomingLimit;
    }


}
