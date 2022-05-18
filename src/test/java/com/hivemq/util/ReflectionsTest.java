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

import org.junit.Test;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Optional;

import static com.hivemq.util.Reflections.getMethodAnnotationFromInterface;
import static org.junit.Assert.assertEquals;

public class ReflectionsTest {

    @Test
    public void test_interface_has_annotation() throws Exception {

        final Optional<TestAnnotation> annotation = getMethodAnnotationFromInterface(TestClass.class.getMethod("doSomething", int.class), TestAnnotation.class);

        assertEquals(true, annotation.isPresent());
    }

    @Test
    public void test_interface_has_no_annotation() throws Exception {

        final Optional<TestAnnotation> annotation = getMethodAnnotationFromInterface(TestClass.class.getMethod("doSomething2", int.class), TestAnnotation.class);

        assertEquals(false, annotation.isPresent());
    }

    @Test
    public void test_interface_overloaded_method_no_annotation() throws Exception {

        final Optional<TestAnnotation> annotation = getMethodAnnotationFromInterface(TestClass.class.getMethod("doSomething", int.class, int.class), TestAnnotation.class);

        assertEquals(false, annotation.isPresent());
    }

    @Test
    public void test_has_no_interface() throws Exception {

        final Optional<TestAnnotation> annotation = getMethodAnnotationFromInterface(Object.class.getMethod("equals", Object.class), TestAnnotation.class);

        assertEquals(false, annotation.isPresent());
    }


    interface TestParentInterface {

        @TestAnnotation
        void doSomething(int param);

        void doSomething(int param, int param2);

        void doSomething2(int param);
    }


    @Retention(RetentionPolicy.RUNTIME)
    public @interface TestAnnotation {
    }

    static class TestClass implements TestParentInterface, Serializable {

        @Override
        public void doSomething(final int param) {

        }

        @Override
        public void doSomething(final int param, final int param2) {

        }

        @Override
        public void doSomething2(final int param) {

        }
    }

}