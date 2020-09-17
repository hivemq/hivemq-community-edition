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
package util;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.Authorizer;
import com.hivemq.extension.sdk.api.auth.SubscriptionAuthorizer;
import com.hivemq.extension.sdk.api.auth.parameter.SubscriptionAuthorizerInput;
import com.hivemq.extension.sdk.api.auth.parameter.SubscriptionAuthorizerOutput;
import com.hivemq.extensions.classloader.IsolatedExtensionClassloader;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
@SuppressWarnings("deprecation")
public class TestAuthorizerUtil {


    @NotNull
    public static SubscriptionAuthorizer getIsolatedSubscriptionAuthorizer(final TemporaryFolder temporaryFolder, @NotNull final ClassLoader classLoader) throws Exception {

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("util.TestAuthorizerUtil$TestSubscriptionAuthorizer");

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        //This classloader contains the classes from the jar file
        final IsolatedExtensionClassloader cl = new IsolatedExtensionClassloader(new URL[]{jarFile.toURI().toURL()}, classLoader);

        final List<Authorizer> authorizers = new ArrayList<>();

        final Class<?> subscriptionAuthorizerClass = cl.loadClass("util.TestAuthorizerUtil$TestSubscriptionAuthorizer");

        final SubscriptionAuthorizer subscriptionAuthorizer = (SubscriptionAuthorizer) subscriptionAuthorizerClass.newInstance();

        return subscriptionAuthorizer;

    }

    public static class TestSubscriptionAuthorizer implements SubscriptionAuthorizer {

        @Override
        public void authorizeSubscribe(@com.hivemq.extension.sdk.api.annotations.NotNull final SubscriptionAuthorizerInput subscriptionAuthorizerInput,
                                       @com.hivemq.extension.sdk.api.annotations.NotNull final SubscriptionAuthorizerOutput subscriptionAuthorizerOutput) {

        }
    }

}
