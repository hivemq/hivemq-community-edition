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
package com.hivemq.bootstrap;

import com.hivemq.configuration.service.entity.TcpListener;
import com.hivemq.exceptions.UnrecoverableException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Dominik Obermaier
 */
public class StartupListenerVerifierTest {

    @Test(expected = UnrecoverableException.class)
    public void test_verifier_verify_only_listener_failed() throws Exception {
        final ListenerStartupInformation failed =
                ListenerStartupInformation.failedListenerStartup(new TcpListener(2000, "0.0.0.0"),
                        new RuntimeException("reason"));
        new StartupListenerVerifier(List.of(failed)).verifyAndPrint();
    }

    @Test(expected = UnrecoverableException.class)
    public void test_verifier_verify_all_listeners_failed() throws Exception {
        final ListenerStartupInformation failed =
                ListenerStartupInformation.failedListenerStartup(new TcpListener(2000, "0.0.0.0"),
                        new RuntimeException("reason"));
        final ListenerStartupInformation failed2 =
                ListenerStartupInformation.failedListenerStartup(new TcpListener(1234, "0.0.0.0"),
                        new RuntimeException("anotherreason"));

        new StartupListenerVerifier(List.of(failed, failed2)).verifyAndPrint();
    }

    @Test
    public void test_verifier_verify_some_listeners_failed() throws Exception {
        final ListenerStartupInformation failed =
                ListenerStartupInformation.failedListenerStartup(new TcpListener(2000, "0.0.0.0"),
                        new RuntimeException("reason"));
        final ListenerStartupInformation success =
                ListenerStartupInformation.successfulListenerStartup(new TcpListener(1234, "0.0.0.0"));

        new StartupListenerVerifier(List.of(failed, success)).verifyAndPrint();

        //We don't receive an exception so everything is good
    }

    @Test(expected = UnrecoverableException.class)
    public void test_verifier_verify_empty_listeners() throws Exception {
        new StartupListenerVerifier(new ArrayList<ListenerStartupInformation>()).verifyAndPrint();
    }

    @Test(expected = NullPointerException.class)
    public void test_verifier_doesnt_accept_null() throws Exception {
        new StartupListenerVerifier(null);
    }
}
