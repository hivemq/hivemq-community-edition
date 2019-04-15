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
import com.hivemq.annotations.Validate;
import com.hivemq.configuration.Validator;
import com.hivemq.configuration.service.exception.ValidationError;
import com.hivemq.util.Reflections;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * An interceptor which validates a method against the Validator Annotation on a interface.
 * It's important that only {@link Validate} annotations
 * on the interface are considered, direct method implementation annotations are ignored
 * <p>
 * Make sure to use this interceptor only in conjunction with
 * the {@link com.hivemq.configuration.ioc.aop.ParentInterfaceAnnotationMatcher}
 *
 * @author Dominik Obermaier
 */
public class ValidatorInterceptor implements MethodInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ValidatorInterceptor.class);

    @Override
    public Object invoke(final MethodInvocation invocation) throws Throwable {

        final Optional<Validate> annotation = Reflections.getMethodAnnotationFromInterface(invocation.getMethod(), Validate.class);
        if (!annotation.isPresent()) {
            //This should never happen because the Matcher should prohibit that the interceptor is called
            //when the annotation is not present
            log.warn("{} does not have a @Validate annotation in the interface definition. Proceeding without validation", invocation.getMethod().toString());
            return invocation.proceed();
        } else {

            final Validator validator = getValidator(annotation);

            final Object[] arguments = invocation.getArguments();
            if (arguments.length != 1) {
                log.warn("Validators are only applicable for methods with 1 argument. The method {} has {} arguments. Ignoring validation", invocation.getMethod().toString(), arguments.length);
            } else {

                final Object argument = arguments[0];
                //The actual validation

                List<ValidationError> validate = Collections.emptyList();
                try {

                    String name = annotation.get().name();
                    if (name.trim().isEmpty()) {
                        name = invocation.getMethod().toGenericString();
                    }

                    validate = validator.validate(argument, name);
                } catch (final Exception e) {
                    //Guard from exceptions in the validate method. Better be safe than sorry
                    log.error("Validator {} threw an exception. Ignoring validation results.", validator.getClass().getCanonicalName(), e);
                }

                if (!validate.isEmpty()) {
                    for (final ValidationError validationError : validate) {
                        log.error(validationError.getMessage());
                    }
                    return null;
                }
            }
        }
        return invocation.proceed();
    }

    @SuppressWarnings("deprecation")
    private Validator getValidator(final Optional<Validate> annotation) throws InstantiationException, IllegalAccessException {
        final Validate validateAnnotation = annotation.get();
        final Class<? extends Validator> validatorClass = validateAnnotation.value();

        return validatorClass.newInstance();
    }
}
