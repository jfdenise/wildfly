/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.undertow.graal;

import org.jboss.as.controller.graal.PreMainInitializer;
import org.wildfly.extension.undertow.ServiceLoaderInitializer;

/**
 *
 * @author jdenise
 */
public class PreMainInitializerImpl implements PreMainInitializer {

    @Override
    public void init() {
        ServiceLoaderInitializer.init();
    }

}
