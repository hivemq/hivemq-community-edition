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
package com.hivemq.bootstrap.ioc;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.hivemq.mqtt.topic.TopicMatcher;
import io.netty.util.concurrent.EventExecutorGroup;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertNotSame;

/**
 * @author Florian Limpöck
 * @since 4.1.0
 */
public class HiveMQMainModuleTest {

    @Test
    public void test_topic_matcher_not_same() {

        final Injector injector = Guice.createInjector(Stage.PRODUCTION,
                new HiveMQMainModule(),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(EventExecutorGroup.class).toInstance(Mockito.mock(EventExecutorGroup.class));
                    }
                });

        final TopicMatcher instance1 = injector.getInstance(TopicMatcher.class);
        final TopicMatcher instance2 = injector.getInstance(TopicMatcher.class);

        assertNotSame(instance1, instance2);

    }
}