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

import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

public final class Reflections {

    private Reflections() {
    }

    /**
     * This method is a convenient way to ask for a specific annotation on the interface
     * method declaration.
     * <p>
     * Returns the concrete annotation for a given method from interface methods.
     * Java does not inherit annotations form interface methods to implementations, so we need to
     * find this out by our own.
     *
     * @param method     the method on the concrete implementation of an interface
     * @param annotation the annotation to search for
     * @return an Optional of the concrete annotation
     */
    public static <T extends Annotation> Optional<T> getMethodAnnotationFromInterface(
            final @NotNull Method method, final @NotNull Class<? extends T> annotation) {
        final Class<?>[] interfaces = method.getDeclaringClass().getInterfaces();

        for (final Class<?> anInterface : interfaces) {

            final Method[] interfaceMethods = anInterface.getMethods();

            for (final Method interfaceMethod : interfaceMethods) {
                if (methodSignatureEquals(method, interfaceMethod)) {

                    return Optional.ofNullable(interfaceMethod.getAnnotation(annotation));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Checks if two methods signatures are equal. In order to be considered to be equal, the following
     * conditions must be true.
     * <ul>
     * <li>The <i>return type</i> is equal</li>
     * <li>The <i>method name</i> is equal</li>
     * <li>The <i>parameter types</i> are equal</li>
     * </ul>
     * In order to match, the <i>class</i> does <b>not</b> need to be equal. This allows
     * e.g. to check for method equality in interfaces and subclasses.
     *
     * @param method
     * @param anotherMethod
     * @return if the methods are equal in regard to the conditions above
     */
    private static boolean methodSignatureEquals(final @NotNull Method method, final @NotNull Method anotherMethod) {
        if (method.getReturnType().equals(anotherMethod.getReturnType())) {
            if (method.getName().equals(anotherMethod.getName())) {
                return Arrays.equals(method.getParameterTypes(), anotherMethod.getParameterTypes());
            }
        }
        return false;
    }
}
