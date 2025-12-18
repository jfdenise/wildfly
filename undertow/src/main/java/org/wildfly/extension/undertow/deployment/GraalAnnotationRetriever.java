/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.undertow.deployment;

import io.undertow.servlet.api.AnnotationRetriever;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import org.jboss.modules.ModuleClassLoader;

public class GraalAnnotationRetriever implements AnnotationRetriever {

    public static final GraalAnnotationRetriever INSTANCE = new GraalAnnotationRetriever();

    private GraalAnnotationRetriever() {
    }

    @Override
    public Annotation getAnnotation(Class<?> clazz, Class<? extends Annotation> annotType) throws RuntimeException {
        ModuleClassLoader loader = (ModuleClassLoader) clazz.getClassLoader();
        return loader.getModule().getAnnotation(clazz, annotType);
    }

    @Override
    public Annotation getAnnotation(Class<?> clazz, Method m, Class<? extends Annotation> annotType) throws RuntimeException {
        ModuleClassLoader loader = (ModuleClassLoader) clazz.getClassLoader();
        return loader.getModule().getAnnotation(clazz, m, annotType);
    }

    @Override
    public Annotation getDeclaredAnnotation(Class<?> clazz, Class<? extends Annotation> annotType) throws RuntimeException {
        ModuleClassLoader loader = (ModuleClassLoader) clazz.getClassLoader();
        return loader.getModule().getAnnotation(clazz, annotType);
    }

    @Override
    public Annotation getDeclaredAnnotation(Class<?> clazz, Method m, Class<? extends Annotation> annotType) throws RuntimeException {
        ModuleClassLoader loader = (ModuleClassLoader) clazz.getClassLoader();
        return loader.getModule().getAnnotation(clazz, m, annotType);
    }

    @Override
    public boolean isAnnotationPresent(Class<?> clazz, Method m, Class<? extends Annotation> annotType) throws RuntimeException {
        ModuleClassLoader loader = (ModuleClassLoader) clazz.getClassLoader();
        return loader.getModule().getAnnotation(clazz, m, annotType) != null;
    }

    @Override
    public Annotation[][] getParameterAnnotations(Class<?> clazz, Method m) throws RuntimeException {
        ModuleClassLoader loader = (ModuleClassLoader) clazz.getClassLoader();
        return loader.getModule().getParameterAnnotations(clazz, m);
    }

    @Override
    public Method[] getDeclaredMethods(Class<?> clazz) throws RuntimeException {
        ModuleClassLoader loader = (ModuleClassLoader) clazz.getClassLoader();
        return loader.getModule().getDeclaredMethods(clazz);
    }

}
