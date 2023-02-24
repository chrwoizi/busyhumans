package com.c5000.mongopa;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A mandatory class annotation that can be used to associate a POJO class with a database collection.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface MpaModel {
    String collection() default "";
}