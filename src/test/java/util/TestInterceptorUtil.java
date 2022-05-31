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
import com.hivemq.extension.sdk.api.interceptor.Interceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundInput;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundOutput;
import com.hivemq.extension.sdk.api.interceptor.subscribe.SubscribeInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.subscribe.parameter.SubscribeInboundInput;
import com.hivemq.extension.sdk.api.interceptor.subscribe.parameter.SubscribeInboundOutput;
import com.hivemq.extensions.classloader.IsolatedExtensionClassloader;
import org.junit.rules.TemporaryFolder;

import java.util.LinkedList;
import java.util.List;

/**
 * @since 4.0.0
 */
public class TestInterceptorUtil {

    // legacy
    public static @NotNull List<Interceptor> getIsolatedInterceptors(
            final @NotNull TemporaryFolder temporaryFolder) throws Exception {
        return getIsolatedInterceptors(List.of(TestPublishInboundInterceptor.class,
                TestSubscriberInboundInterceptor.class), temporaryFolder);
    }

    public static <T extends Interceptor> @NotNull T getIsolatedInterceptor(
            final @NotNull Class<T> type, final @NotNull TemporaryFolder temporaryFolder) throws Exception {
        return getIsolatedInterceptors(List.of(type), temporaryFolder).get(0);
    }

    public static <T extends Interceptor> @NotNull List<T> getIsolatedInterceptors(
            final @NotNull List<Class<? extends T>> types, final @NotNull TemporaryFolder temporaryFolder)
            throws Exception {
        try (final IsolatedExtensionClassloader cl = IsolatedExtensionClassloaderUtil.buildClassLoader(temporaryFolder.getRoot().toPath(),
                types.toArray(new Class[0]))) {
            final LinkedList<T> list = new LinkedList<>();
            for (final Class<? extends T> type : types) {
                final Class<?> clazz = cl.loadClass(type.getName());
                //noinspection unchecked
                list.add((T) clazz.getDeclaredConstructor().newInstance());
            }
            return list;
        }
    }

    public static class TestPublishInboundInterceptor implements PublishInboundInterceptor {

        @Override
        public void onInboundPublish(
                final @NotNull PublishInboundInput input, final @NotNull PublishInboundOutput output) {
        }
    }

    public static class TestSubscriberInboundInterceptor implements SubscribeInboundInterceptor {

        @Override
        public void onInboundSubscribe(
                final @NotNull SubscribeInboundInput subscribeInboundInput,
                final @NotNull SubscribeInboundOutput subscribeInboundOutput) {
        }
    }
}
