/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.undertow;

import io.undertow.servlet.api.ServletInfo;
import org.apache.jasper.servlet.JspServlet;

/**
 *
 * @author jdenise
 */
public class ServiceLoaderInitializer {

    static boolean JSP_INITIALIZED;
    static ServletInfo JSP_SERVLET;
    static {
        try {
            Class.forName("org.apache.jasper.compiler.JspRuntimeContext", true, ServiceLoaderInitializer.class.getClassLoader());
            JSP_INITIALIZED = true;
        } catch (Exception ex) {
            // OK.
            System.out.println("JSP NOT INITIALIZED");
        }

        JSP_SERVLET = new ServletInfo("jsp", JspServlet.class);
    }

    public static void checkJsp() throws ClassNotFoundException {
        if (!JSP_INITIALIZED) {
            throw new ClassNotFoundException("JSP class not found");
        }
    }

    public static ServletInfo getJspServletInfo() {
        return JSP_SERVLET;
    }

}
