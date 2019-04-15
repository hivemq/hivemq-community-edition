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

package com.hivemq.configuration.ioc.aop;

import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.matcher.Matchers;
import com.hivemq.annotations.Validate;
import com.hivemq.configuration.Validator;
import com.hivemq.configuration.service.exception.ValidationError;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Dominik Obermaier
 */
public class ValidatorInterceptorTest {

    private Injector injector;

    @Before
    public void setUp() throws Exception {
        injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {

                bind(Service.class).to(ServiceImpl.class);
                bindInterceptor(Matchers.any(), Matchers.any(), new ValidatorInterceptor());
            }
        });

    }

    @Test
    public void test_validation_annotation_not_present() throws Exception {

        final Service instance = injector.getInstance(Service.class);
        final int returnValue = instance.doSomething2(10);

        assertEquals(10, returnValue);
    }

    @Test
    public void test_validation_succeeds() throws Exception {

        final Service instance = injector.getInstance(Service.class);
        final int returnValue = instance.doSomething(10);

        assertEquals(10, returnValue);
    }

    @Test
    public void test_validation_fails() throws Exception {

        final Service instance = injector.getInstance(Service.class);
        final int returnValue = instance.doSomething(-10);

        //The method was not called, so we are getting the default value of the method
        assertEquals(0, returnValue);
    }

    @Test
    public void test_invalid_validation_annotation() throws Exception {
        final Service instance = injector.getInstance(Service.class);
        final int returnValue = instance.invalidMethod(-100, -50);

        //The validator did not prohibit to execute the method
        assertEquals(-150, returnValue);
    }

    @Test
    public void test_exception_in_validator() throws Exception {
        final Service instance = injector.getInstance(Service.class);
        final int returnValue = instance.exceptionMethod(10);

        //The validator did not prohibit to execute the method
        assertEquals(10, returnValue);
    }


    @Test
    public void test_validation_without_name_uses_method_name() throws Exception {

        final Service instance = injector.getInstance(Service.class);
        instance.noNameMethod("blub");

        assertEquals(ServiceImpl.class.getMethod("noNameMethod", String.class).toString(), NameValidator.currentName);
    }

    @Test
    public void test_validation_with_name_uses_method_name() throws Exception {

        final Service instance = injector.getInstance(Service.class);
        instance.nameMethod("blub");

        assertEquals("test", NameValidator.currentName);
    }

    @Test
    public void test_validation_with_invalid_type() throws Exception {

        final Service instance = injector.getInstance(Service.class);
        final String returnValue = instance.invalidType("blub");

        assertEquals("blub", returnValue);
    }

    interface Service {

        @Validate(TestIntValidator.class)
        int doSomething(int input);

        int doSomething2(int input);

        @Validate(TestIntValidator.class)
        int invalidMethod(int input, int input2);

        @Validate(ExceptionValidator.class)
        int exceptionMethod(int input);

        @Validate(NameValidator.class)
        String noNameMethod(String string);

        @Validate(value = NameValidator.class, name = "test")
        String nameMethod(final String string);

        @Validate(TestIntValidator.class)
        String invalidType(final String invalidType);

    }

    static class ServiceImpl implements Service {
        @Override
        public int doSomething(final int input) {
            return input;
        }

        @Override
        public int doSomething2(final int input) {
            return input;
        }

        @Override
        public int invalidMethod(final int input, final int input2) {
            return input + input2;
        }

        @Override
        public int exceptionMethod(final int input) {
            return input;
        }

        @Override
        public String noNameMethod(final String input) {
            return input;
        }

        @Override
        public String nameMethod(final String input) {
            return input;
        }

        @Override
        public String invalidType(final String input) {
            return input;
        }
    }

    static class TestIntValidator implements Validator<Integer> {
        @Override
        public List<ValidationError> validate(final Integer parameter, final String name) {
            if (parameter < 0) {
                return Lists.newArrayList(new ValidationError("Error!"));
            }
            return Collections.emptyList();
        }
    }


    static class ExceptionValidator implements Validator<Integer> {

        @Override
        public List<ValidationError> validate(final Integer parameter, final String name) {
            throw new RuntimeException("THIS IS A EXCEPTION!");
        }
    }

    static class NameValidator implements Validator<String> {

        static String currentName;

        @Override
        public List<ValidationError> validate(final String parameter, final String name) {
            currentName = name;
            return Lists.newArrayList(new ValidationError(name));
        }


    }

}