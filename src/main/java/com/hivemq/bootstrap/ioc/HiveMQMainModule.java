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

package com.hivemq.bootstrap.ioc;

import com.hivemq.common.annotations.Internal;
import com.hivemq.mqtt.topic.TokenizedTopicMatcher;
import com.hivemq.mqtt.topic.TopicMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.prefs.Preferences;

/**
 * @author Dominik Obermaier
 * @author Christoph Schäbel
 */
public class HiveMQMainModule extends SingletonModule {
    private static final Logger log = LoggerFactory.getLogger(HiveMQMainModule.class);

    public HiveMQMainModule() {
        super(HiveMQMainModule.class);
    }

    @Override
    protected void configure() {
        bind(TopicMatcher.class).to(TokenizedTopicMatcher.class);
        bind(Preferences.class).annotatedWith(Internal.class).toInstance(Preferences.userRoot().node("hivemq"));
    }

}
