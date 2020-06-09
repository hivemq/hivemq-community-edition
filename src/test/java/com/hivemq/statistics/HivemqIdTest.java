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
package com.hivemq.statistics;

import com.hivemq.configuration.info.SystemInformation;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author Christoph Schäbel
 */
public class HivemqIdTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private SystemInformation systemInformation;

    private HivemqId hivemqId;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        when(systemInformation.getDataFolder()).thenReturn(temporaryFolder.getRoot());

        hivemqId = new HivemqId(systemInformation);
    }


    @Test
    public void test_no_id_present() {

        hivemqId.postConstruct();

        final String hivemqIdString = this.hivemqId.getHivemqId();
        assertEquals(36, hivemqIdString.length());
        assertEquals(true, new File(temporaryFolder.getRoot(), "meta.id").exists());

        //test if the same id is returned
        assertEquals(hivemqIdString, hivemqId.getHivemqId());
    }

    @Test
    public void test_read_from_file() throws Exception {

        final File idFile = new File(temporaryFolder.getRoot(), "meta.id");
        final String id = "123456789012345678901234567890123456";
        FileUtils.writeStringToFile(idFile, id, StandardCharsets.UTF_8);

        hivemqId.postConstruct();

        assertEquals(id, hivemqId.getHivemqId());
        assertEquals(true, idFile.exists());

        //test if the same id is returned
        assertEquals(id, hivemqId.getHivemqId());
    }

}