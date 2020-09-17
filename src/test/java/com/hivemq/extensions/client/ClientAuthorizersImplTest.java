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
package com.hivemq.extensions.client;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.SubscriptionAuthorizer;
import com.hivemq.extensions.ExtensionPriorityComparator;
import com.hivemq.extensions.classloader.IsolatedExtensionClassloader;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.TestAuthorizerUtil;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Christoph Schäbel
 */
@SuppressWarnings("ALL")
public class ClientAuthorizersImplTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    ExtensionPriorityComparator extensionPriorityComparator;

    private ClientAuthorizersImpl authorizers;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        when(extensionPriorityComparator.compare(any(), any())).thenAnswer(invocation -> {
            return Integer.compare(invocation.getArguments()[0].hashCode(), invocation.getArguments()[1].hashCode());
        });
        authorizers = new ClientAuthorizersImpl(extensionPriorityComparator);
    }

    @Test
    public void test_put_get_authorizers() throws Exception {

        final @NotNull SubscriptionAuthorizer authorizer1 = TestAuthorizerUtil.getIsolatedSubscriptionAuthorizer(temporaryFolder, this.getClass().getClassLoader());
        final @NotNull SubscriptionAuthorizer authorizer2 = TestAuthorizerUtil.getIsolatedSubscriptionAuthorizer(temporaryFolder, this.getClass().getClassLoader());

        final IsolatedExtensionClassloader classloader = (IsolatedExtensionClassloader) authorizer1.getClass().getClassLoader();
        final IsolatedExtensionClassloader anotherClassLoader = (IsolatedExtensionClassloader) authorizer2.getClass().getClassLoader();


        authorizers.put("extension-1", authorizer1);
        authorizers.put("extension-2", authorizer2);


        final Map<String, SubscriptionAuthorizer> map = authorizers.getSubscriptionAuthorizersMap();

        assertEquals(2, map.size());

        assertSame(authorizer1, map.get("extension-1"));
        assertSame(authorizer2, map.get("extension-2"));
    }

    @Test
    public void test_remove_authorizers() throws Exception {

        final @NotNull SubscriptionAuthorizer authorizer1 = TestAuthorizerUtil.getIsolatedSubscriptionAuthorizer(temporaryFolder, this.getClass().getClassLoader());
        final @NotNull SubscriptionAuthorizer authorizer2 = TestAuthorizerUtil.getIsolatedSubscriptionAuthorizer(temporaryFolder, this.getClass().getClassLoader());

        final IsolatedExtensionClassloader classloader = (IsolatedExtensionClassloader) authorizer1.getClass().getClassLoader();
        final IsolatedExtensionClassloader anotherClassLoader = (IsolatedExtensionClassloader) authorizer2.getClass().getClassLoader();


        authorizers.put("extension-1", authorizer1);
        authorizers.put("extension-2", authorizer2);

        assertEquals(2, authorizers.getSubscriptionAuthorizersMap().size());

        authorizers.removeAllForPlugin(classloader);

        final Map<String, SubscriptionAuthorizer> map = authorizers.getSubscriptionAuthorizersMap();

        assertEquals(1, map.size());
        assertSame(authorizer2, map.get("extension-2"));
    }


}