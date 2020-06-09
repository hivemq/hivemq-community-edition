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

import com.google.common.collect.Lists;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.Collections;
import java.util.List;

/**
 * Listens for triggered user events
 */
public class CheckUserEventTriggeredOnSuper extends ChannelInboundHandlerAdapter {

    private boolean triggered;

    List<Object> objectList = Collections.synchronizedList(Lists.newArrayList());

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) throws Exception {
        triggered = true;
        objectList.add(evt);
        super.userEventTriggered(ctx, evt);
    }

    public boolean isTriggered() {
        return triggered;
    }

    public List<Object> getTriggeredEvents() {
        return objectList;
    }
}