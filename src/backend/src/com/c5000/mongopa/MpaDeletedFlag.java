package com.c5000.mongopa;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * An optional field annotation that can be used on boolean fields which denote an entity as deleted.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface MpaDeletedFlag {
}