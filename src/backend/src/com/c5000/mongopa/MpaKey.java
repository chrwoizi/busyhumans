package com.c5000.mongopa;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A mandatory field annotation that can be used to associate a field with a key in a key-value store.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface MpaKey {
}