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

import com.hivemq.exceptions.UnrecoverableException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author Christoph Schäbel
 */
public class EnvVarUtilTest {

    @Mock
    EnvVarUtil envVarUtil;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        when(envVarUtil.replaceEnvironmentVariablePlaceholders(anyString())).thenCallRealMethod();
    }

    @Test
    public void test_getValue_existing() throws Exception {
        when(envVarUtil.getValue(anyString())).thenCallRealMethod();

        final HashMap<String, String> map = new HashMap<>();
        map.put("TEST_EXISTING_ENVVAR", "iamset");
        setTempEnvVars(map);

        final String result = envVarUtil.getValue("TEST_EXISTING_ENVVAR");

        assertEquals("iamset", result);
    }

    @Test
    public void test_getValue_existing_java_prop() throws Exception {
        when(envVarUtil.getValue(anyString())).thenCallRealMethod();

        System.setProperty("test.existing.envvar", "iamset2");

        final String result = envVarUtil.getValue("test.existing.envvar");

        assertEquals("iamset2", result);
    }

    @Test
    public void test_getValue_existing_both() throws Exception {
        when(envVarUtil.getValue(anyString())).thenCallRealMethod();

        final HashMap<String, String> map = new HashMap<>();
        map.put("test.existing.both", "iamset");
        setTempEnvVars(map);

        System.setProperty("test.existing.both", "iamset2");

        final String result = envVarUtil.getValue("test.existing.both");

        //expect System.property to win
        assertEquals("iamset2", result);
    }

    @Test
    public void test_getValue_non_existing() throws Exception {
        when(envVarUtil.getValue(anyString())).thenCallRealMethod();

        final String result = envVarUtil.getValue("TEST_NON_EXISTING_ENVVAR");

        assertEquals(null, result);
    }

    @Test
    public void test_replaceEnvironmentVariablePlaceholders() throws Exception {
        when(envVarUtil.getValue(eq("VALUE1"))).thenReturn("value$1");
        when(envVarUtil.getValue(eq("VALUE2"))).thenReturn("2");
        when(envVarUtil.getValue(eq("VALUE3"))).thenReturn("value-_/!\"\\'3!§%&/()=?`*,;.:[]|{}");

        final String testString = "<test1><test2 id=\"VALUE1\"><test3>${VALUE1}</test3><test4>${VALUE2}</test4><test5>${VALUE3}</test5></test2></test1>";

        final String result = envVarUtil.replaceEnvironmentVariablePlaceholders(testString);

        final String expected = "<test1><test2 id=\"VALUE1\"><test3>value$1</test3><test4>2</test4><test5>value-_/!\"\\'3!§%&/()=?`*,;.:[]|{}</test5></test2></test1>";

        assertEquals(expected, result);
    }

    @Test(expected = UnrecoverableException.class)
    public void test_replaceEnvironmentVariablePlaceholders_unknown_varname() throws Exception {

        when(envVarUtil.getValue(eq("VALUE1"))).thenReturn("value1");

        final String testString = "<test1>${VALUE1}</test1><test2>${VALUE2}</test2>";

        final String result = envVarUtil.replaceEnvironmentVariablePlaceholders(testString);
    }

    /**
     * Modifies the in-memory map which is returned when System.getenv is called.
     * Does not set Env-Vars at all
     *
     * @param newenv the new Map which should be uses by System.getenv
     * @throws Exception
     */
    private void setTempEnvVars(final Map<String, String> newenv) throws Exception {
        final Class[] classes = Collections.class.getDeclaredClasses();
        final Map<String, String> env = System.getenv();
        for (final Class cl : classes) {
            if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                final Field field = cl.getDeclaredField("m");
                field.setAccessible(true);
                final Object obj = field.get(env);
                final Map<String, String> map = (Map<String, String>) obj;
                map.clear();
                map.putAll(newenv);
            }
        }
    }

}