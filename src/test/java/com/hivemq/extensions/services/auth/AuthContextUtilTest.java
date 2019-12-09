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

package com.hivemq.extensions.services.auth;

import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

/**
 * @author Florian Limpöck
 * @since 4.2.0
 */
public class AuthContextUtilTest {

    @Test
    public void test_success_fallback() {

        final AuthOutput output = Mockito.mock(AuthOutput.class);

        when(output.isAsync()).thenReturn(true);
        when(output.isTimedOut()).thenReturn(true);
        when(output.getTimeoutFallback()).thenReturn(TimeoutFallback.SUCCESS);

        AuthContextUtil.checkTimeout(output);

        verify(output).nextByTimeout();

    }

    @Test
    public void test_failure_fallback() {

        final AuthOutput output = Mockito.mock(AuthOutput.class);

        when(output.isAsync()).thenReturn(true);
        when(output.isTimedOut()).thenReturn(true);
        when(output.getTimeoutFallback()).thenReturn(TimeoutFallback.FAILURE);

        AuthContextUtil.checkTimeout(output);

        verify(output).failByTimeout();

    }

    @Test
    public void test_check_succeeds_not_async() {

        final AuthOutput output = Mockito.mock(AuthOutput.class);

        when(output.isAsync()).thenReturn(false);
        when(output.isTimedOut()).thenReturn(true);
        when(output.getTimeoutFallback()).thenReturn(TimeoutFallback.FAILURE);

        AuthContextUtil.checkTimeout(output);

        verify(output, never()).nextByTimeout();
        verify(output, never()).failByTimeout();

    }

    @Test
    public void test_check_succeeds_not_timedout() {

        final AuthOutput output = Mockito.mock(AuthOutput.class);

        when(output.isAsync()).thenReturn(true);
        when(output.isTimedOut()).thenReturn(false);
        when(output.getTimeoutFallback()).thenReturn(TimeoutFallback.FAILURE);

        AuthContextUtil.checkTimeout(output);

        verify(output, never()).nextByTimeout();
        verify(output, never()).failByTimeout();

    }
}