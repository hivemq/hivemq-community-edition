package com.hivemq.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation indicates that a method must be executed by the SingleWriterService.
 *
 * @author Lukas Brandl
 * @since 4.3.3
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface ExecuteInSingleWriter {
    String value() default "";
}