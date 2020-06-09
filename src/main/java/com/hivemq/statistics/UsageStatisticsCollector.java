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
package com.hivemq.statistics;

/**
 * @author Christoph Schäbel
 */
public interface UsageStatisticsCollector {

    /**
     * Collect a statistic by a given statistic type as a JsonString.
     *
     * @param statisticsType the statistic type.
     * @return the statistic as a Json String.
     */
    String getJson(String statisticsType) throws Exception;
}
