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

import com.google.inject.ProvisionException;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import com.hivemq.bootstrap.ioc.SingletonModule;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * The Guice module which allows to use lifecycle annotations.
 * Lifecycle annotations which are supported at the moment are
 * <br>
 * * {@link javax.annotation.PostConstruct}
 * * {@link javax.annotation.PreDestroy}
 *
 * @author Dominik Obermaier
 * @author Silvio Giebl
 */
public class LifecycleModule extends SingletonModule<Class<LifecycleModule>> {

    private static final Logger log = LoggerFactory.getLogger(LifecycleModule.class);

    private final @NotNull LifecycleRegistry lifecycleRegistry;

    /**
     * This class stores in the LifecycleRegistry for Singleton classes if their lifecycle methods are already invoked.
     * Therefore should only one LifecycleModule object exist for the lifetime of the Application.
     */
    public LifecycleModule() {
        super(LifecycleModule.class);

        lifecycleRegistry = new LifecycleRegistry();
    }

    @Override
    protected void configure() {

        bind(LifecycleRegistry.class).toInstance(lifecycleRegistry);
        bind(LifecycleShutdownRegistration.class).asEagerSingleton();

        bindListener(Matchers.any(), new TypeListener() {
            @Override
            public <I> void hear(final TypeLiteral<I> type, final TypeEncounter<I> encounter) {
                executePostConstruct(encounter, type.getRawType());
            }
        });
    }

    private <I> void executePostConstruct(
            final @NotNull TypeEncounter<I> encounter, final @NotNull Class<? super I> rawType) {

        //We're going recursive up to every superclass until we hit Object.class
        if (rawType.getSuperclass() != null) {
            executePostConstruct(encounter, rawType.getSuperclass());
        }

        final boolean singleton = isSingleton(rawType);
        final Method postConstructFound = findPostConstruct(rawType);
        final Method preDestroy = findPreDestroy(rawType);

        if (singleton) {
            lifecycleRegistry.addSingletonClass(rawType);
        }

        if (postConstructFound != null) {
            if (lifecycleRegistry.canInvokePostConstruct(rawType)) {
                invokePostConstruct(encounter, postConstructFound);
            }
        }
        if (preDestroy != null && lifecycleRegistry.canInvokePreDestroy(rawType)) {
            addPreDestroyToRegistry(encounter, preDestroy, lifecycleRegistry);
        }
    }

    private static <I> boolean isSingleton(final @NotNull Class<? super I> rawType) {
        return rawType.isAnnotationPresent(javax.inject.Singleton.class)
                || rawType.isAnnotationPresent(com.google.inject.Singleton.class)
                || rawType.isAnnotationPresent(com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton.class);
    }

    private static <I> Method findPostConstruct(final @NotNull Class<? super I> rawType) {
        Method postConstructFound = null;
        for (final Method method : rawType.getDeclaredMethods()) {
            if (method.isAnnotationPresent(PostConstruct.class)) {
                if (method.getParameterTypes().length != 0) {
                    throw new ProvisionException("A method annotated with @PostConstruct must not have any parameters");
                }
                if (method.getExceptionTypes().length > 0) {
                    throw new ProvisionException("A method annotated with @PostConstruct must not throw any checked exceptions");
                }
                if (Modifier.isStatic(method.getModifiers())) {
                    throw new ProvisionException("A method annotated with @PostConstruct must not be static");
                }
                if (postConstructFound != null) {
                    throw new ProvisionException("More than one @PostConstruct method found for class " + rawType);
                }
                postConstructFound = method;
            }
        }
        return postConstructFound;
    }

    private static <I> Method findPreDestroy(final @NotNull Class<? super I> rawType) {
        Method predestroyFound = null;
        for (final Method method : rawType.getDeclaredMethods()) {
            if (method.isAnnotationPresent(PreDestroy.class)) {
                if (method.getParameterTypes().length != 0) {
                    throw new ProvisionException("A method annotated with @PreDestroy must not have any parameters");
                }
                if (predestroyFound != null) {
                    throw new ProvisionException("More than one @PreDestroy method found for class " + rawType);
                }
                predestroyFound = method;
            }
        }
        return predestroyFound;
    }

    private static <I> void invokePostConstruct(
            final @NotNull TypeEncounter<I> encounter, final @NotNull Method postConstruct) {

        encounter.register((InjectionListener<I>) injectee -> {
            try {
                postConstruct.setAccessible(true);
                postConstruct.invoke(injectee);
            } catch (final IllegalAccessException | InvocationTargetException e) {
                if (e.getCause() instanceof UnrecoverableException) {
                    if (((UnrecoverableException) e.getCause()).isShowException()) {
                        log.error("An unrecoverable Exception occurred. Exiting HiveMQ", e);
                    }
                    System.exit(1);
                }
                throw new ProvisionException("An error occurred while calling @PostConstruct", e);
            }
        });
    }

    private static <I> void addPreDestroyToRegistry(
            final @NotNull TypeEncounter<I> encounter,
            final @NotNull Method preDestroy,
            final @NotNull LifecycleRegistry lifecycleRegistry) {
        encounter.register((InjectionListener<I>) injectionListener -> lifecycleRegistry.addPreDestroyMethod(preDestroy, injectionListener));
    }
}
