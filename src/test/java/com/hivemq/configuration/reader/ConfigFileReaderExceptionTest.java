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
package com.hivemq.configuration.reader;

import com.google.common.io.Files;
import com.hivemq.exceptions.UnrecoverableException;
import org.junit.Test;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;

public class ConfigFileReaderExceptionTest extends AbstractConfigurationTest {

    @Test
    public void test_readConfig_run_with_unrecoverable_exception() throws IOException {
        final String contents = "" +
                "<hivemq>" +
                "</hivemq>";
        Files.write(contents.getBytes(UTF_8), xmlFile);
        doAnswer(invocation -> {
            throw new UnrecoverableException();
        }).when(envVarUtil).replaceEnvironmentVariablePlaceholders(any(String.class));
        reader.applyConfig();
    }

}