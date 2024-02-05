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

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class DnsResolverTest {
    public static final String ALIAS_1 = "alias1";
    public static final String ALIAS_2 = "alias2";
    public static final String ALIAS_3 = "alias3";
    public static final String TEST_EXAMPLE_COM = "test.example.com";
    public static final String TEST_OTHER_COM = "test.other.com";
    public static final String TEST_FOO_BAR = "test.foo.bar";
    public static final String WILDCARD_EXAMPLE_COM = "*.example.com";

    @Test
    public void test_resolve_simple_dns_name() {
        final DnsResolver dnsResolver =
                new DnsResolver(Map.of(TEST_EXAMPLE_COM, ALIAS_1, TEST_OTHER_COM, ALIAS_2, TEST_FOO_BAR, ALIAS_3));

        final String resolve = dnsResolver.resolve(TEST_EXAMPLE_COM);

        assertNotNull(resolve);
        assertEquals(ALIAS_1, resolve);
    }

    @Test
    public void test_resolve_non_matching_dns_name() {
        final DnsResolver dnsResolver =
                new DnsResolver(Map.of(TEST_EXAMPLE_COM, ALIAS_1, TEST_OTHER_COM, ALIAS_2, TEST_FOO_BAR, ALIAS_3));

        final String resolve = dnsResolver.resolve("other.example.com");

        assertNull(resolve);
    }

    @Test
    public void test_resolve_wildcard_dns_name() {
        final DnsResolver dnsResolver =
                new DnsResolver(Map.of(WILDCARD_EXAMPLE_COM, ALIAS_1, TEST_OTHER_COM, ALIAS_2, TEST_FOO_BAR, ALIAS_3));

        final String resolve = dnsResolver.resolve(TEST_EXAMPLE_COM);

        assertNotNull(resolve);
        assertEquals(ALIAS_1, resolve);
    }

    @Test
    public void test_resolve_nested_wildcard_dns_name() {
        final DnsResolver dnsResolver =
                new DnsResolver(Map.of(WILDCARD_EXAMPLE_COM, ALIAS_1, "test.example.com", ALIAS_2, TEST_FOO_BAR, ALIAS_3));

        final String resolve = dnsResolver.resolve("sub.test.example.com");

        assertNotNull(resolve);
        assertEquals(ALIAS_1, resolve);
    }

    @Test
    public void test_resolve_non_matching_wildcard_dns_name() {
        final DnsResolver dnsResolver = new DnsResolver(Map.of("*.other.com", ALIAS_2, TEST_FOO_BAR, ALIAS_3));

        final String resolve = dnsResolver.resolve("test.example.com");

        assertNull(resolve);
    }

    @Test
    public void test_prefer_full_cert_before_wildcard_cert() {
        final DnsResolver dnsResolver =
                new DnsResolver(Map.of("test.example.com", ALIAS_1, WILDCARD_EXAMPLE_COM, ALIAS_2, TEST_FOO_BAR, ALIAS_3));

        final String resolve = dnsResolver.resolve("test.example.com");

        assertNotNull(resolve);
        assertEquals(ALIAS_1, resolve);
    }

    @Test
    public void test_prefer_best_matching_wildcard_cert() {
        final DnsResolver dnsResolver =
                new DnsResolver(Map.of(WILDCARD_EXAMPLE_COM, ALIAS_1, "*.test.example.com", ALIAS_2, TEST_FOO_BAR, ALIAS_3));

        final String resolve = dnsResolver.resolve("sub.test.example.com");

        assertNotNull(resolve);
        assertEquals(ALIAS_2, resolve);
    }
}
