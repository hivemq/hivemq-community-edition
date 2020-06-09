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
package com.hivemq.persistence.util;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * @author Lukas Brandl
 */
public class BatchedExceptionTest {

    @Test
    public void test_get_stack_trace() {
        final RuntimeException exception1 = new RuntimeException();
        final RuntimeException exception2 = new RuntimeException();
        final BatchedException batchedException = new BatchedException(Arrays.asList(exception1, exception2));
        final StackTraceElement[] stackTrace = batchedException.getStackTrace();
        final int totalLength = exception1.getStackTrace().length + exception2.getStackTrace().length;
        assertEquals(totalLength, stackTrace.length);
    }
}