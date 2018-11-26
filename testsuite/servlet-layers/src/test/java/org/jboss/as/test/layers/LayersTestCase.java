/*
 * Copyright 2016-2018 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.test.layers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

/**
 *
 * @author jdenise@redhat.com
 */
public class LayersTestCase {
    // Packages that are provisioned but not used (not injected nor referenced).
    // They can be not provisioned.
    private static final String[] NOT_USED = {
        // Un-used
        "org.apache.xml-resolver",
        // Un-used
        "org.jboss.metadata",
        // Un-used
        "javax.sql.api",
        // Un-used
        "javax.validation.api",
        // Un-used
        "javax.activation.api",
    };
    // Set of packages that are not present in the tested config
    private static final String[] NOT_IN_CONFIG = {
        // deprecated
        "org.jboss.as.threads",
        // discovery not configured in default config
        "org.wildfly.discovery",
        // discovery not configured in default config
        "org.wildfly.extension.discovery"
    };
    // Packages that are not referenced from the module graph of a provisioned
    // default config but needed.
    private static final String[] NOT_REFERENCED = {
        //  injected by server in UndertowHttpManagementService
        "org.jboss.as.domain-http-error-context",
        // injected by logging
        "org.jboss.logging.jul-to-slf4j-stub",
        // injected by logging
        "org.slf4j.ext",
        // injected by logging
        "ch.qos.cal10n",
        // tooling
        "org.jboss.as.domain-add-user",
        // Brought by galleon FP config
        "org.jboss.as.product",
        // Brought by galleon FP config
        "org.jboss.as.standalone",
        // injected by ee
        "javax.json.bind.api",
        // injected by ee
        "org.eclipse.yasson",
        // injected by ee
        "org.wildfly.naming",
        };

    @Test
    public void test() throws Exception {
        String root = System.getProperty("layers.install.root");
        Set<String> notUsed = new HashSet<>();
        notUsed.addAll(Arrays.asList(NOT_USED));
        notUsed.addAll(Arrays.asList(NOT_IN_CONFIG));
        Set<String> notReferenced = new HashSet<>();
        notReferenced.addAll(Arrays.asList(NOT_REFERENCED));
        notReferenced.addAll(notUsed);
        LayersTest.test(root, notReferenced, notUsed);
    }
}
