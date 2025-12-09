/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.undertow;

import io.undertow.servlet.api.ServletInfo;
import java.util.List;
import java.util.Map;
import org.apache.jasper.servlet.JspServlet;
import org.jboss.as.controller.graal.GraalRecorder;
import org.wildfly.extension.undertow.deployment.JspInitializationListener;
import org.wildfly.extension.undertow.deployment.UndertowDeploymentInfoService;

/**
 *
 * @author jdenise
 */
public class ServiceLoaderInitializer {

    static boolean JSP_INITIALIZED;
    static ServletInfo JSP_SERVLET;
    static UndertowDeploymentInfoService INFO_SERVICE;
    static JspInitializationListener JSP_LISTENER;
    static {
        try {
            System.out.println("UndertowDeploymentInfoService " + UndertowDeploymentInfoService.DEFAULT_SERVLET_NAME);
            System.out.println("CONTEXT CLASSLOADER " + Thread.currentThread().getContextClassLoader());
            System.out.println("ServiceLoaderInitializer.class.getClassLoader() " + ServiceLoaderInitializer.class.getClassLoader());
            Class.forName("org.apache.jasper.compiler.JspRuntimeContext", true, ServiceLoaderInitializer.class.getClassLoader());
            JSP_INITIALIZED = true;
            System.out.println("INITIALIZED JSP LISTENER");
            JSP_LISTENER.init();
        } catch (Throwable ex) {
            // OK.
            ex.printStackTrace();
            System.out.println("JSP NOT INITIALIZED");
        }

        JSP_SERVLET = new ServletInfo("jsp", JspServlet.class);
    }
    public static void init(Map<String, List<GraalRecorder.Record>> map) throws Exception {
        System.out.println("JSP_SERVLET " + JSP_SERVLET);
        UndertowDeploymentInfoService.init(map);
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
