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
package com.hivemq.lifecycle;

import com.google.inject.AbstractModule;
import com.google.inject.ConfigurationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.ProvisionException;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingletonModule;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.info.SystemInformationImpl;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class LifecycleModuleTest {

    private LifecycleModule lifecycleModule;
    private Injector injector;

    @Before
    public void setUp() throws Exception {
        lifecycleModule = new LifecycleModule();
        createInjector();
    }

    private void createInjector() {
        injector = Guice.createInjector(lifecycleModule, new LazySingletonModule(), new AbstractModule() {
            @Override
            protected void configure() {
                bind(SystemInformation.class).toInstance(new SystemInformationImpl());
            }
        });
    }

    /* **********************
     * @PostConstruct tests *
     * ******************+**/

    @Test
    public void test_postConstruct_executed() throws Exception {

        final WithPostConstruct instance = injector.getInstance(WithPostConstruct.class);
        assertEquals(0, instance.getLatch().getCount());
    }

    @Test
    public void postConstruct_whenNormalClass_thenEveryTime() throws Exception {

        final WithPostConstruct instance = injector.getInstance(WithPostConstruct.class);
        assertEquals(0, instance.getLatch().getCount());

        createInjector();

        final WithPostConstruct instance2 = injector.getInstance(WithPostConstruct.class);
        assertEquals(0, instance2.getLatch().getCount());
    }

    @Test
    public void postConstruct_whenJavaxSingletonClass_thenOnce() throws Exception {

        final JavaxSingletonWithPostConstruct instance = injector.getInstance(JavaxSingletonWithPostConstruct.class);
        assertEquals(0, instance.getLatch().getCount());

        createInjector();

        final JavaxSingletonWithPostConstruct instance2 = injector.getInstance(JavaxSingletonWithPostConstruct.class);
        assertEquals(1, instance2.getLatch().getCount());
    }

    @Test
    public void postConstruct_whenGoogleSingletonClass_thenOnce() throws Exception {

        final GoogleSingletonWithPostConstruct instance = injector.getInstance(GoogleSingletonWithPostConstruct.class);
        assertEquals(0, instance.getLatch().getCount());

        createInjector();

        final GoogleSingletonWithPostConstruct instance2 = injector.getInstance(GoogleSingletonWithPostConstruct.class);
        assertEquals(1, instance2.getLatch().getCount());
    }

    @Test
    public void postConstruct_whenLazySingletonClass_thenOnce() throws Exception {

        final LazySingletonWithPostConstruct instance = injector.getInstance(LazySingletonWithPostConstruct.class);
        assertEquals(0, instance.getLatch().getCount());

        createInjector();

        final LazySingletonWithPostConstruct instance2 = injector.getInstance(LazySingletonWithPostConstruct.class);
        assertEquals(1, instance2.getLatch().getCount());
    }

    @Test
    public void test_default_visibility_postConstruct_executed() throws Exception {

        final WithDefaultVisibilityPostConstruct instance =
                injector.getInstance(WithDefaultVisibilityPostConstruct.class);

        assertEquals(0, instance.getLatch().getCount());
    }

    @Test
    public void test_private_visibility_postConstruct_executed() throws Exception {

        final WithPrivateVisibilityPostConstruct instance =
                injector.getInstance(WithPrivateVisibilityPostConstruct.class);

        assertEquals(0, instance.getLatch().getCount());
    }

    @Test(expected = ConfigurationException.class)
    public void test_two_postconstructs() throws Exception {

        injector.getInstance(WithTwoPostConstructs.class);
    }

    @Test(expected = ConfigurationException.class)
    public void test_postconstruct_params() throws Exception {

        injector.getInstance(WithPostConstructParameters.class);
    }

    @Test(expected = ConfigurationException.class)
    public void test_postconstruct_with_checked_exception() throws Exception {

        injector.getInstance(WithDeclaredException.class);
    }

    @Test(expected = ConfigurationException.class)
    public void test_postconstruct_with_static_post_method() throws Exception {

        injector.getInstance(WithStaticPostConstruct.class);
    }

    @Test(expected = ProvisionException.class)
    public void test_postconstruct_with_runtime_exception() throws Exception {

        injector.getInstance(WithRuntimeException.class);
    }

    static class WithPostConstruct {

        private final CountDownLatch latch = new CountDownLatch(1);

        @PostConstruct
        public void postConstruct() {
            latch.countDown();
        }

        public CountDownLatch getLatch() {
            return latch;
        }
    }

    @javax.inject.Singleton
    static class JavaxSingletonWithPostConstruct {

        private final CountDownLatch latch = new CountDownLatch(1);

        @PostConstruct
        public void postConstruct() {
            latch.countDown();
        }

        public CountDownLatch getLatch() {
            return latch;
        }
    }

    @com.google.inject.Singleton
    static class GoogleSingletonWithPostConstruct {

        private final CountDownLatch latch = new CountDownLatch(1);

        @PostConstruct
        public void postConstruct() {
            latch.countDown();
        }

        public CountDownLatch getLatch() {
            return latch;
        }
    }

    @LazySingleton
    static class LazySingletonWithPostConstruct {

        private final CountDownLatch latch = new CountDownLatch(1);

        @PostConstruct
        public void postConstruct() {
            latch.countDown();
        }

        public CountDownLatch getLatch() {
            return latch;
        }
    }

    static class WithPostConstructParameters {

        @PostConstruct
        public void postConstruct(final String param) {
        }
    }

    static class WithTwoPostConstructs {

        @PostConstruct
        public void postConstruct() {
        }

        @PostConstruct
        public void postConstruct2() {
        }
    }

    static class WithDeclaredException {

        @PostConstruct
        public void postConstruct() throws Exception {
        }
    }

    static class WithStaticPostConstruct {

        @PostConstruct
        public static void postConstruct() {
        }
    }

    static class WithRuntimeException {

        @PostConstruct
        public void postConstruct() {
            throw new RuntimeException("Ex");
        }
    }

    static class WithDefaultVisibilityPostConstruct {

        private final CountDownLatch latch = new CountDownLatch(1);

        @PostConstruct
        void postConstruct() {
            latch.countDown();
        }

        public CountDownLatch getLatch() {
            return latch;
        }
    }

    static class WithPrivateVisibilityPostConstruct {

        private final CountDownLatch latch = new CountDownLatch(1);

        @PostConstruct
        private void postConstruct() {
            latch.countDown();
        }

        public CountDownLatch getLatch() {
            return latch;
        }
    }

    /* ***************************************
     * @Postconstruct tests with inheritance *
     * ***************************************/

    @Test
    public void test_postConstruct_inheritance() throws Exception {

        final ChildClassWithPostConstruct instance = injector.getInstance(ChildClassWithPostConstruct.class);

        assertEquals(0, instance.getLatch().getCount());
        assertEquals(0, instance.getParentLatch().getCount());
    }

    @Test
    public void test_postConstruct_inheritance_overwritten_super_postconstruct() throws Exception {

        final ChildClassWithPostConstructOverwritten instance =
                injector.getInstance(ChildClassWithPostConstructOverwritten.class);

        assertEquals(0, instance.getLatch().getCount());
        assertEquals(1, instance.getParentLatch().getCount());
    }

    static abstract class ParentAbstractClassWithPostConstruct {
        private final CountDownLatch parentLatch = new CountDownLatch(1);

        @PostConstruct
        public void parentPostConstruct() {
            parentLatch.countDown();
        }

        CountDownLatch getParentLatch() {
            return parentLatch;
        }
    }

    static class ChildClassWithPostConstruct extends ParentAbstractClassWithPostConstruct {

        private final CountDownLatch latch = new CountDownLatch(1);

        @PostConstruct
        public void postConstruct() {
            latch.countDown();
        }

        public CountDownLatch getLatch() {
            return latch;
        }
    }

    static class ChildClassWithPostConstructOverwritten extends ParentAbstractClassWithPostConstruct {

        private final CountDownLatch latch = new CountDownLatch(1);

        @PostConstruct
        @Override
        public void parentPostConstruct() {
            latch.countDown();
        }

        public CountDownLatch getLatch() {
            return latch;
        }
    }

    /* **********************
     * @PreDestroy tests *
     * ******************+**/

    @Test
    public void test_predestroy() throws Exception {
        final WithPreDestroy instance = injector.getInstance(WithPreDestroy.class);
        assertEquals(1, instance.getLatch().getCount());

        final LifecycleRegistry registry = injector.getInstance(LifecycleRegistry.class);
        registry.executePreDestroy().get(1, TimeUnit.SECONDS);

        assertEquals(0, instance.getLatch().getCount());
    }

    @Test
    public void preDestroy_whenNormalClass_thenEveryTime() throws Exception {

        final WithPreDestroy instance = injector.getInstance(WithPreDestroy.class);

        final LifecycleRegistry registry = injector.getInstance(LifecycleRegistry.class);
        registry.executePreDestroy().get(1, TimeUnit.SECONDS);

        assertEquals(0, instance.getLatch().getCount());

        createInjector();

        final WithPreDestroy instance2 = injector.getInstance(WithPreDestroy.class);

        final LifecycleRegistry registry2 = injector.getInstance(LifecycleRegistry.class);
        registry2.executePreDestroy().get(1, TimeUnit.SECONDS);

        assertEquals(0, instance2.getLatch().getCount());
    }

    @Test
    public void preDestroy_whenJavaxSingletonClass_thenOnce() throws Exception {

        final JavaxSingletonWithPreDestroy instance = injector.getInstance(JavaxSingletonWithPreDestroy.class);

        final LifecycleRegistry registry = injector.getInstance(LifecycleRegistry.class);
        registry.executePreDestroy().get(1, TimeUnit.SECONDS);

        assertEquals(0, instance.getLatch().getCount());

        createInjector();

        final JavaxSingletonWithPreDestroy instance2 = injector.getInstance(JavaxSingletonWithPreDestroy.class);

        final LifecycleRegistry registry2 = injector.getInstance(LifecycleRegistry.class);
        registry2.executePreDestroy().get(1, TimeUnit.SECONDS);

        assertEquals(1, instance2.getLatch().getCount());
    }

    @Test
    public void preDestroy_whenGoogleSingletonClass_thenOnce() throws Exception {

        final GoogleSingletonWithPreDestroy instance = injector.getInstance(GoogleSingletonWithPreDestroy.class);

        final LifecycleRegistry registry = injector.getInstance(LifecycleRegistry.class);
        registry.executePreDestroy().get(1, TimeUnit.SECONDS);

        assertEquals(0, instance.getLatch().getCount());

        createInjector();

        final GoogleSingletonWithPreDestroy instance2 = injector.getInstance(GoogleSingletonWithPreDestroy.class);

        final LifecycleRegistry registry2 = injector.getInstance(LifecycleRegistry.class);
        registry2.executePreDestroy().get(1, TimeUnit.SECONDS);

        assertEquals(1, instance2.getLatch().getCount());
    }

    @Test
    public void preDestroy_whenLazySingletonClass_thenOnce() throws Exception {

        final LazySingletonWithPreDestroy instance = injector.getInstance(LazySingletonWithPreDestroy.class);

        final LifecycleRegistry registry = injector.getInstance(LifecycleRegistry.class);
        registry.executePreDestroy().get(1, TimeUnit.SECONDS);

        assertEquals(0, instance.getLatch().getCount());

        createInjector();

        final LazySingletonWithPreDestroy instance2 = injector.getInstance(LazySingletonWithPreDestroy.class);

        final LifecycleRegistry registry2 = injector.getInstance(LifecycleRegistry.class);
        registry2.executePreDestroy().get(1, TimeUnit.SECONDS);

        assertEquals(1, instance2.getLatch().getCount());
    }

    @Test
    public void test_predestroy_two_classes() throws Exception {
        final WithPreDestroy instance = injector.getInstance(WithPreDestroy.class);
        final WithPreDestroy instance2 = injector.getInstance(WithPreDestroy.class);
        assertEquals(1, instance.getLatch().getCount());
        assertEquals(1, instance2.getLatch().getCount());

        final LifecycleRegistry registry = injector.getInstance(LifecycleRegistry.class);
        registry.executePreDestroy().get(1, TimeUnit.SECONDS);

        assertEquals(0, instance.getLatch().getCount());
        assertEquals(0, instance2.getLatch().getCount());
    }

    @Test(expected = ConfigurationException.class)
    public void test_predestroy_with_two_predestroys() throws Exception {
        injector.getInstance(WithTwoPreDestroys.class);
    }

    @Test(expected = ConfigurationException.class)
    public void test_predestroy_with_parameters() throws Exception {
        injector.getInstance(WithPreDestroyParameters.class);
    }

    static class WithPreDestroy {

        private final CountDownLatch latch = new CountDownLatch(1);

        @PreDestroy
        public void preDestroy() {
            latch.countDown();
        }

        public CountDownLatch getLatch() {
            return latch;
        }
    }

    @javax.inject.Singleton
    static class JavaxSingletonWithPreDestroy {

        private final CountDownLatch latch = new CountDownLatch(1);

        @PreDestroy
        public void preDestroy() {
            latch.countDown();
        }

        public CountDownLatch getLatch() {
            return latch;
        }
    }

    @com.google.inject.Singleton
    static class GoogleSingletonWithPreDestroy {

        private final CountDownLatch latch = new CountDownLatch(1);

        @PreDestroy
        public void preDestroy() {
            latch.countDown();
        }

        public CountDownLatch getLatch() {
            return latch;
        }
    }

    @LazySingleton
    static class LazySingletonWithPreDestroy {

        private final CountDownLatch latch = new CountDownLatch(1);

        @PreDestroy
        public void preDestroy() {
            latch.countDown();
        }

        public CountDownLatch getLatch() {
            return latch;
        }
    }

    static class WithTwoPreDestroys {

        @PreDestroy
        public void preDestroy() {
        }

        @PreDestroy
        public void preDestroy2() {
        }

    }

    static class WithPreDestroyParameters {

        @PreDestroy
        public void preDestroy(final String param) {
        }
    }

    /* **************************************
     * @PreDestroy and @PostConstruct tests *
     * ***********************************+**/

    @Test
    public void test_predestroy_and_postconstruct() throws Exception {
        final WithPreDestroyAndPostConstruct instance = injector.getInstance(WithPreDestroyAndPostConstruct.class);
        assertEquals(1, instance.getPreDestroyLatch().getCount());
        assertEquals(0, instance.getPostConstructLatch().getCount());

        final LifecycleRegistry registry = injector.getInstance(LifecycleRegistry.class);
        registry.executePreDestroy().get(1, TimeUnit.SECONDS);

        assertEquals(0, instance.getPreDestroyLatch().getCount());
    }

    @Test
    public void lifecycleMethods_whenNormalClass_thenEveryTime() throws Exception {
        final WithPreDestroyAndPostConstruct instance = injector.getInstance(WithPreDestroyAndPostConstruct.class);
        assertEquals(1, instance.getPreDestroyLatch().getCount());
        assertEquals(0, instance.getPostConstructLatch().getCount());

        final LifecycleRegistry registry = injector.getInstance(LifecycleRegistry.class);
        registry.executePreDestroy().get(1, TimeUnit.SECONDS);

        assertEquals(0, instance.getPreDestroyLatch().getCount());

        createInjector();

        final WithPreDestroyAndPostConstruct instance2 = injector.getInstance(WithPreDestroyAndPostConstruct.class);
        assertEquals(1, instance2.getPreDestroyLatch().getCount());
        assertEquals(0, instance2.getPostConstructLatch().getCount());

        final LifecycleRegistry registry2 = injector.getInstance(LifecycleRegistry.class);
        registry2.executePreDestroy().get(1, TimeUnit.SECONDS);

        assertEquals(0, instance2.getPreDestroyLatch().getCount());
    }

    @Test
    public void lifecycleMethods_whenJavaxSingletonClass_thenOnce() throws Exception {
        final JavaxSingletonWithPreDestroyAndPostConstruct instance =
                injector.getInstance(JavaxSingletonWithPreDestroyAndPostConstruct.class);
        assertEquals(1, instance.getPreDestroyLatch().getCount());
        assertEquals(0, instance.getPostConstructLatch().getCount());

        final LifecycleRegistry registry = injector.getInstance(LifecycleRegistry.class);
        registry.executePreDestroy().get(1, TimeUnit.SECONDS);

        assertEquals(0, instance.getPreDestroyLatch().getCount());

        createInjector();

        final JavaxSingletonWithPreDestroyAndPostConstruct instance2 =
                injector.getInstance(JavaxSingletonWithPreDestroyAndPostConstruct.class);
        assertEquals(1, instance2.getPostConstructLatch().getCount());

        final LifecycleRegistry registry2 = injector.getInstance(LifecycleRegistry.class);
        registry2.executePreDestroy().get(1, TimeUnit.SECONDS);

        assertEquals(1, instance2.getPreDestroyLatch().getCount());
    }

    @Test
    public void lifecycleMethods_whenGoogleSingletonClass_thenOnce() throws Exception {
        final GoogleSingletonWithPreDestroyAndPostConstruct instance =
                injector.getInstance(GoogleSingletonWithPreDestroyAndPostConstruct.class);
        assertEquals(1, instance.getPreDestroyLatch().getCount());
        assertEquals(0, instance.getPostConstructLatch().getCount());

        final LifecycleRegistry registry = injector.getInstance(LifecycleRegistry.class);
        registry.executePreDestroy().get(1, TimeUnit.SECONDS);

        assertEquals(0, instance.getPreDestroyLatch().getCount());

        createInjector();

        final GoogleSingletonWithPreDestroyAndPostConstruct instance2 =
                injector.getInstance(GoogleSingletonWithPreDestroyAndPostConstruct.class);
        assertEquals(1, instance2.getPostConstructLatch().getCount());

        final LifecycleRegistry registry2 = injector.getInstance(LifecycleRegistry.class);
        registry2.executePreDestroy().get(1, TimeUnit.SECONDS);

        assertEquals(1, instance2.getPreDestroyLatch().getCount());
    }

    @Test
    public void lifecycleMethods_whenLazySingletonClass_thenOnce() throws Exception {
        final LazySingletonWithPreDestroyAndPostConstruct instance =
                injector.getInstance(LazySingletonWithPreDestroyAndPostConstruct.class);
        assertEquals(1, instance.getPreDestroyLatch().getCount());
        assertEquals(0, instance.getPostConstructLatch().getCount());

        final LifecycleRegistry registry = injector.getInstance(LifecycleRegistry.class);
        registry.executePreDestroy().get(1, TimeUnit.SECONDS);

        assertEquals(0, instance.getPreDestroyLatch().getCount());

        createInjector();

        final LazySingletonWithPreDestroyAndPostConstruct instance2 =
                injector.getInstance(LazySingletonWithPreDestroyAndPostConstruct.class);
        assertEquals(1, instance2.getPostConstructLatch().getCount());

        final LifecycleRegistry registry2 = injector.getInstance(LifecycleRegistry.class);
        registry2.executePreDestroy().get(1, TimeUnit.SECONDS);

        assertEquals(1, instance2.getPreDestroyLatch().getCount());
    }

    static class WithPreDestroyAndPostConstruct {

        private final CountDownLatch preDestroyLatch = new CountDownLatch(1);
        private final CountDownLatch postConstructLatch = new CountDownLatch(1);

        @PreDestroy
        public void preDestroy() {
            preDestroyLatch.countDown();
        }

        @PostConstruct
        public void postConstruct() {
            postConstructLatch.countDown();
        }

        CountDownLatch getPreDestroyLatch() {
            return preDestroyLatch;
        }

        CountDownLatch getPostConstructLatch() {
            return postConstructLatch;
        }
    }

    @javax.inject.Singleton
    static class JavaxSingletonWithPreDestroyAndPostConstruct {

        private final CountDownLatch preDestroyLatch = new CountDownLatch(1);
        private final CountDownLatch postConstructLatch = new CountDownLatch(1);

        @PreDestroy
        public void preDestroy() {
            preDestroyLatch.countDown();
        }

        @PostConstruct
        public void postConstruct() {
            postConstructLatch.countDown();
        }

        CountDownLatch getPreDestroyLatch() {
            return preDestroyLatch;
        }

        CountDownLatch getPostConstructLatch() {
            return postConstructLatch;
        }
    }

    @com.google.inject.Singleton
    static class GoogleSingletonWithPreDestroyAndPostConstruct {

        private final CountDownLatch preDestroyLatch = new CountDownLatch(1);
        private final CountDownLatch postConstructLatch = new CountDownLatch(1);

        @PreDestroy
        public void preDestroy() {
            preDestroyLatch.countDown();
        }

        @PostConstruct
        public void postConstruct() {
            postConstructLatch.countDown();
        }

        CountDownLatch getPreDestroyLatch() {
            return preDestroyLatch;
        }

        CountDownLatch getPostConstructLatch() {
            return postConstructLatch;
        }
    }

    @LazySingleton
    static class LazySingletonWithPreDestroyAndPostConstruct {

        private final CountDownLatch preDestroyLatch = new CountDownLatch(1);
        private final CountDownLatch postConstructLatch = new CountDownLatch(1);

        @PreDestroy
        public void preDestroy() {
            preDestroyLatch.countDown();
        }

        @PostConstruct
        public void postConstruct() {
            postConstructLatch.countDown();
        }

        CountDownLatch getPreDestroyLatch() {
            return preDestroyLatch;
        }

        CountDownLatch getPostConstructLatch() {
            return postConstructLatch;
        }
    }
}
