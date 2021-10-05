package com.hivemq.annotations;

import java.lang.annotation.*;

/**
 * This annotation indicates that a method or the methods of a class must be executed in the Eventloop of the respective
 * client.
 *
 * @author Abdullah Imal
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.CLASS)
@Inherited
public @interface ExecuteInEventloop {
    String value() default "";
}