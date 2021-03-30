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
package com.hivemq.configuration.service.impl;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import util.LogbackCapturingAppender;

import static org.junit.Assert.*;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author Waldemar Ruck
 * @since 4.0
 */
public class SecurityConfigurationServiceImplTest {

    private final SecurityConfigurationServiceImpl securityConfigurationService = new SecurityConfigurationServiceImpl();

    private LogbackCapturingAppender logCapture;

    @Before
    public void setup() {
        initMocks(this);
        final Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        logCapture = LogbackCapturingAppender.Factory.weaveInto(logger);
    }

    @After
    public void tearDown() throws Exception {
        LogbackCapturingAppender.Factory.cleanUp();
    }

    @Test
    public void test_defaults() {
        assertTrue(securityConfigurationService.allowRequestProblemInformation());
        assertTrue(securityConfigurationService.allowServerAssignedClientId());
        assertFalse(securityConfigurationService.payloadFormatValidation());
        assertTrue(securityConfigurationService.validateUTF8());

        assertFalse(logCapture.isLogCaptured());
    }

    @Test
    public void test_setting_properties() {

        securityConfigurationService.setAllowRequestProblemInformation(false);
        assertTrue(logCapture.isLogCaptured());
        assertEquals(Level.DEBUG, logCapture.getLastCapturedLog().getLevel());
        assertEquals("Setting allow-problem-information to false", logCapture.getLastCapturedLog().getFormattedMessage());

        securityConfigurationService.setAllowServerAssignedClientId(false);
        assertEquals(Level.DEBUG, logCapture.getLastCapturedLog().getLevel());
        assertEquals("Setting allow server assigned client identifier to false", logCapture.getLastCapturedLog().getFormattedMessage());

        securityConfigurationService.setPayloadFormatValidation(true);
        assertEquals(Level.DEBUG, logCapture.getLastCapturedLog().getLevel());
        assertEquals("Setting payload format validation to true", logCapture.getLastCapturedLog().getFormattedMessage());

        securityConfigurationService.setValidateUTF8(false);
        assertEquals(Level.DEBUG, logCapture.getLastCapturedLog().getLevel());
        assertEquals("Setting validate UTF-8 to false", logCapture.getLastCapturedLog().getFormattedMessage());

        assertFalse(securityConfigurationService.allowRequestProblemInformation());
        assertFalse(securityConfigurationService.allowServerAssignedClientId());
        assertTrue(securityConfigurationService.payloadFormatValidation());
        assertFalse(securityConfigurationService.validateUTF8());
    }
}