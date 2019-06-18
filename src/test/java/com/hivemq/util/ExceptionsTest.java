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

package com.hivemq.util;

import com.google.common.collect.ImmutableList;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.persistence.util.BatchedException;
import org.junit.Test;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Dominik Obermaier
 */
public class ExceptionsTest {

    @Test
    public void test_rethrowUnrecoverableCause_no_unrecoverable_cause() throws Exception {
        final RuntimeException nestedThrowable = getNestedThrowable(10, new IllegalAccessException());
        Exceptions.rethrowUnrecoverableCause(nestedThrowable);
    }

    @Test
    public void test_rethrowUnrecoverableCause_null_cause() throws Exception {
        final RuntimeException nestedThrowable = getNestedThrowable(10, null);
        final RuntimeException runtimeException = new RuntimeException((Throwable) null);
        Exceptions.rethrowUnrecoverableCause(nestedThrowable);
        Exceptions.rethrowUnrecoverableCause(runtimeException);
    }

    @Test(expected = UnrecoverableException.class)
    public void test_rethrowUnrecoverableCause_unrecoverable_cause() throws Exception {
        final RuntimeException nestedThrowable = getNestedThrowable(10, new UnrecoverableException());
        Exceptions.rethrowUnrecoverableCause(nestedThrowable);
    }

    @Test
    public void test_rethrowUnrecoverableCause_throwable_is_unrecoverable_itself() throws Exception {
        Exceptions.rethrowUnrecoverableCause(new UnrecoverableException());
    }

    @Test
    public void test_rethrowUnrecoverableCause_throwable_is_null() throws Exception {
        Exceptions.rethrowUnrecoverableCause(null);
    }

    @Test
    public void test_is_connection_closed_exception() throws Exception{
        assertTrue(Exceptions.isConnectionClosedException(new ClosedChannelException()));
        assertTrue(Exceptions.isConnectionClosedException(new SSLException("abc")));
        //native Io Exception cannot be instantiated without native transport
        //assertTrue(Exceptions.isConnectionClosedException(mock(Errors.NativeIoException.class)));
        assertTrue(Exceptions.isConnectionClosedException(new IOException("Broken pipe")));
        assertTrue(Exceptions.isConnectionClosedException(new IOException("Protocol wrong type for socket")));
        assertTrue(Exceptions.isConnectionClosedException(new IOException()));

        assertFalse(Exceptions.isConnectionClosedException(new RuntimeException()));
    }

    @Test
    public void test_is_connection_closed_exception_batched() throws Exception {
        final BatchedException batchedException1 = new BatchedException(
                ImmutableList.of(new ClosedChannelException(), new ClosedChannelException()));
        assertTrue(Exceptions.isConnectionClosedException(batchedException1));
        final BatchedException batchedException2 = new BatchedException(ImmutableList.of(new ClosedChannelException(), new RuntimeException()));
        assertFalse(Exceptions.isConnectionClosedException(batchedException2));

    }

    private static RuntimeException getNestedThrowable(final int nest, final Throwable nested) {
        RuntimeException runtimeException = new RuntimeException(nested);
        for (int i = 0; i < nest; i++) {
            final RuntimeException nextRuntimeException = new RuntimeException(runtimeException);
            runtimeException = nextRuntimeException;
        }
        return runtimeException;
    }
}