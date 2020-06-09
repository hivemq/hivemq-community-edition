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
package com.hivemq.util;

import com.hivemq.HiveMQServer;
import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.assertNull;

/**
 * @author Florian Limpöck
 */
public class ResourceUtilsTest {

    @Test
    public void get_resource_null() throws Exception {

        final URL resource = ResourceUtils.getResource(HiveMQServer.class, null);

        assertNull(resource);

    }

    @Test
    public void get_resource_clazz_null() throws Exception {

        final URL resource = ResourceUtils.getResource(null, "path");

        assertNull(resource);

    }

    @Test
    public void get_resource_url_empty() throws Exception {

        final URL resource = ResourceUtils.getResource(HiveMQServer.class, "path");

        assertNull(resource);

    }

}