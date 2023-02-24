package com.c5000.mongopa;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * An optional field annotation that can be used to exclude a field from persistence.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface MpaIgnore {
}