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

package util;

import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.Interceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundInput;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundOutput;
import com.hivemq.extension.sdk.api.interceptor.subscribe.SubscribeInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.subscribe.parameter.SubscribeInboundInput;
import com.hivemq.extension.sdk.api.interceptor.subscribe.parameter.SubscribeInboundOutput;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
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
public class TestInterceptorUtil {


    @NotNull
    public static List<Interceptor> getIsolatedInterceptors(final TemporaryFolder temporaryFolder, @NotNull final ClassLoader classLoader) throws Exception {

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("util.TestInterceptorUtil$TestPublishInboundInterceptor")
                .addClass("util.TestInterceptorUtil$TestSubscriberInboundInterceptor");

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        //This classloader contains the classes from the jar file
        final IsolatedPluginClassloader cl = new IsolatedPluginClassloader(new URL[]{jarFile.toURI().toURL()}, classLoader);

        final List<Interceptor> interceptors = new ArrayList<>();

        final Class<?> publishInboundInterceptorClass = cl.loadClass("util.TestInterceptorUtil$TestPublishInboundInterceptor");
        final Class<?> subscribeInboundInteceptorClass = cl.loadClass("util.TestInterceptorUtil$TestSubscriberInboundInterceptor");

        final PublishInboundInterceptor publishInboundInterceptor = (PublishInboundInterceptor) publishInboundInterceptorClass.newInstance();
        final SubscribeInboundInterceptor subscribeInboundInterceptor = (SubscribeInboundInterceptor) subscribeInboundInteceptorClass.newInstance();
        interceptors.add(publishInboundInterceptor);
        interceptors.add(subscribeInboundInterceptor);

        return interceptors;


    }

    public static class TestPublishInboundInterceptor implements PublishInboundInterceptor {

        @Override
        public void onInboundPublish(@NotNull final PublishInboundInput input, @NotNull final PublishInboundOutput output) {

        }
    }

    public static class TestSubscriberInboundInterceptor implements SubscribeInboundInterceptor {

        @Override
        public void onInboundSubscribe(final @NotNull SubscribeInboundInput subscribeInboundInput, final @NotNull SubscribeInboundOutput subscribeInboundOutput) {

        }
    }

}
