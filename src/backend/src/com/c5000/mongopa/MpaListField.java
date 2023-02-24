package com.c5000.mongopa;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A field annotation that should be used on list fields (type Iterable[_]).
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface MpaListField {
    Class<?> itemType();
}