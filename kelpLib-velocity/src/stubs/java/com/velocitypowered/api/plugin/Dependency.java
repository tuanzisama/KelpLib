package com.velocitypowered.api.plugin;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Minimal compile-time stub of com.velocitypowered.api.plugin.Dependency.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface Dependency {

    String id();

    boolean optional() default false;
}
