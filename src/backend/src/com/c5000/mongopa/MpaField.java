package com.c5000.mongopa;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * An optional field annotation that can be used to define a custom db field name for a field.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface MpaField {
    String value();
}