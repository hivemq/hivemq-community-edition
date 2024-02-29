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
package com.hivemq.security.ssl;

import java.util.Map;
import java.util.Optional;

public class DnsResolver {

    private final Map<String, String> dnsMap;

    DnsResolver(final Map<String, String> dnsMap) {
        this.dnsMap = dnsMap;
    }

    Optional<String> resolve(final String domain) {
        String alias = dnsMap.get(domain);
        if (alias != null) {
            return Optional.of(alias);
        }

        int index = domain.indexOf('.');
        while (index >= 0) {
            final String wildcardDomain = "*" + domain.substring(index);
            alias = dnsMap.get(wildcardDomain);
            if (alias != null) {
                return Optional.of(alias);
            }
            index = domain.indexOf('.', index + 1);
        }

        return Optional.empty();
    }

}
