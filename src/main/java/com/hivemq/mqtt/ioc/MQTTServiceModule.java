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

import com.hivemq.bootstrap.ioc.SingletonModule;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.limitation.TopicAliasLimiter;
import com.hivemq.limitation.TopicAliasLimiterImpl;
import com.hivemq.mqtt.services.*;

/**
 * @author Christoph Schäbel
 */
public class MQTTServiceModule extends SingletonModule {

    public MQTTServiceModule() {
        super(MQTTServiceModule.class);
    }

    @Override
    protected void configure() {

        bind(InternalPublishService.class).to(InternalPublishServiceImpl.class);
        bind(PublishDistributor.class).to(PublishDistributorImpl.class);

        bind(TopicAliasLimiter.class).to(TopicAliasLimiterImpl.class);

        bind(PublishPollService.class).to(PublishPollServiceImpl.class).in(LazySingleton.class);
    }
}
