/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.undertow;

/**
 *
 * @author jdenise
 */
public class ServiceLoaderInitializer {

    static boolean JSP_INITIALIZED;

    static {
        try {
            Class.forName("org.apache.jasper.compiler.JspRuntimeContext", true, ServiceLoaderInitializer.class.getClassLoader());
            JSP_INITIALIZED = true;
        } catch (Exception ex) {
            // OK.
            System.out.println("JSP NOT INITIALIZED");
        }
    }

    public static void checkJsp() throws ClassNotFoundException {
        if (!JSP_INITIALIZED) {
            throw new ClassNotFoundException("JSP class not found");
        }
    }

}
