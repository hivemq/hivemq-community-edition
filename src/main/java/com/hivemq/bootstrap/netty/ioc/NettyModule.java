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
package com.hivemq.bootstrap.netty.ioc;

import com.hivemq.bootstrap.ioc.SingletonModule;
import com.hivemq.bootstrap.netty.ChannelInitializerFactory;
import com.hivemq.bootstrap.netty.ChannelInitializerFactoryImpl;
import com.hivemq.bootstrap.netty.NettyConfiguration;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import javax.inject.Singleton;

/**
 * @author Christoph Schäbel
 * @author Dominik Obermaier
 */
public class NettyModule extends SingletonModule {

    public NettyModule() {
        super(NettyModule.class);
    }

    @Override
    protected void configure() {

        bind(ChannelGroup.class).toInstance(new DefaultChannelGroup(GlobalEventExecutor.INSTANCE));

        //bind server event loops

        bind(NettyConfiguration.class).toProvider(NettyConfigurationProvider.class).in(Singleton.class);

        bind(ChannelInitializerFactory.class).to(ChannelInitializerFactoryImpl.class);
    }

}
