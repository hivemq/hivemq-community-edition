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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The Guice module which allows to use lifecycle annotations.
 * Lifecycle annotations which are supported at the moment are
 * <br/>
 * <br/>
 * * {@link javax.annotation.PostConstruct} <br/>
 * * {@link javax.annotation.PreDestroy}
 *
 * @author Dominik Obermaier
 * @author Silvio Giebl
 */
public class LifecycleModule extends SingletonModule<Class<LifecycleModule>> {

    private static final Logger log = LoggerFactory.getLogger(LifecycleModule.class);

    private static final AtomicReference<LifecycleModule> instance = new AtomicReference<>();

    private final AtomicReference<LifecycleRegistry> lifecycleRegistry = new AtomicReference<>();

    public static LifecycleModule get() {
        instance.compareAndSet(null, new LifecycleModule());
        return instance.get();
    }

    private LifecycleModule() {
        super(LifecycleModule.class);
    }

    @Override
    protected void configure() {

        final boolean newRegistry = lifecycleRegistry.compareAndSet(null, new LifecycleRegistry());
        bind(LifecycleRegistry.class).toInstance(lifecycleRegistry.get());

        if (newRegistry) {
            bind(LifecycleShutdownRegistration.class).asEagerSingleton();
        }

        bindListener(Matchers.any(), new TypeListener() {
            @Override
            public <I> void hear(final TypeLiteral<I> type, final TypeEncounter<I> encounter) {
                executePostConstruct(encounter, type.getRawType(), lifecycleRegistry.get());
            }
        });
    }

    private <I> void executePostConstruct(final TypeEncounter<I> encounter, final Class<? super I> rawType,
                                          final LifecycleRegistry lifecycleRegistry) {
        //We're going recursive up to every superclass until we hit Object.class
        if (rawType.getSuperclass() != null) {
            executePostConstruct(encounter, rawType.getSuperclass(), lifecycleRegistry);
        }

        final Method postConstructFound = findPostConstruct(rawType);
        final Method preDestroy = findPreDestroy(rawType);

        if (postConstructFound != null) {
            invokePostConstruct(encounter, postConstructFound);
        }

        if (preDestroy != null) {
            addPreDestroyToRegistry(encounter, preDestroy, lifecycleRegistry);
        }
    }

    private <I> Method findPostConstruct(final Class<? super I> rawType) {
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

    private <I> Method findPreDestroy(final Class<? super I> rawType) {
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

    private <I> void invokePostConstruct(final TypeEncounter<I> encounter, final Method postConstruct) {
        encounter.register(new InjectionListener<I>() {
            @Override
            public void afterInjection(final I injectee) {
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
            }
        });
    }

    private <I> void addPreDestroyToRegistry(final TypeEncounter<I> encounter, final Method preDestroy, final LifecycleRegistry registry) {
        encounter.register(new InjectionListener<I>() {
            @Override
            public void afterInjection(final I injectee) {
                registry.addPreDestroyMethod(preDestroy, injectee);

            }
        });
    }
}
