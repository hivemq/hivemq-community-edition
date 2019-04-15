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

import com.google.common.base.Optional;
import com.google.inject.matcher.AbstractMatcher;
import com.hivemq.util.Reflections;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * A {@link com.google.inject.matcher.Matcher} which matches if the parent interface
 * is annotated with a specific annotation.
 *
 * @author Dominik Obermaier
 */
public class ParentInterfaceAnnotationMatcher extends AbstractMatcher<Method> {

    private final Class<? extends Annotation> annotation;

    public ParentInterfaceAnnotationMatcher(final Class<? extends Annotation> annotation) {
        this.annotation = annotation;
    }

    @Override
    public boolean matches(final Method method) {
        final Optional<? extends Annotation> annotation = Reflections.getMethodAnnotationFromInterface(method, this.annotation);

        return annotation.isPresent();
    }

}