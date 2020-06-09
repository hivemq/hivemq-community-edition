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
package com.hivemq.diagnostic;

import com.google.inject.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class DiagnosticModuleTest {

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    @Test
    public void test_diagnostic_mode_enabled() throws Exception {
        System.setProperty("diagnosticMode", "true");
        try {

            Guice.createInjector(new DiagnosticModule());
            fail();
        } catch (final CreationException e) {
            //If Guice tries to create the dependency chain for the Module, then the diagnostic mode is enabled
        }
    }

    @Test
    public void test_diagnostic_mode_disabled() throws Exception {
        System.setProperty("diagnosticMode", "false");

        final Injector injector = Guice.createInjector(new DiagnosticModule());

        final Binding<DiagnosticMode> binding = injector.getExistingBinding(Key.get(DiagnosticMode.class));
        assertNull(binding);
    }

    @Test
    public void test_diagnostic_mode_not_set() throws Exception {

        final Injector injector = Guice.createInjector(new DiagnosticModule());

        final Binding<DiagnosticMode> binding = injector.getExistingBinding(Key.get(DiagnosticMode.class));
        assertNull(binding);
    }
}