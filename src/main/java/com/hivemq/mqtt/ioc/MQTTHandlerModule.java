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
package com.hivemq.mqtt.ioc;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Injector;
import com.hivemq.bootstrap.ioc.SingletonModule;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.handler.connack.MqttConnackerImpl;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnectorImpl;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;

import javax.inject.Singleton;

import static com.hivemq.configuration.service.InternalConfigurations.MQTT_EVENT_EXECUTOR_THREAD_COUNT;

/**
 * @author Dominik Obermaier
 */
public class MQTTHandlerModule extends SingletonModule<Class<MQTTHandlerModule>> {

    private final @NotNull Injector persistenceInjector;

    public MQTTHandlerModule(final @NotNull Injector persistenceInjector) {
        super(MQTTHandlerModule.class);
        this.persistenceInjector = persistenceInjector;
    }

    @Override
    protected void configure() {
        final DefaultEventExecutorGroup mqttHandlerWorker = new DefaultEventExecutorGroup(MQTT_EVENT_EXECUTOR_THREAD_COUNT.get(), new ThreadFactoryBuilder().
                setNameFormat("hivemq-event-executor-%d").build());

        bind(EventExecutorGroup.class).toInstance(mqttHandlerWorker);

        bind(MessageDroppedService.class).toInstance(persistenceInjector.getInstance(MessageDroppedService.class));

        bind(MqttServerDisconnector.class).to(MqttServerDisconnectorImpl.class).in(Singleton.class);
        bind(MqttConnacker.class).to(MqttConnackerImpl.class).in(Singleton.class);

    }
}
