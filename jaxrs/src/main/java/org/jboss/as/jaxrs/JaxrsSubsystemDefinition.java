/*
 * Copyright (C) 2014 Red Hat, inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.jboss.as.jaxrs;

import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;

import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.RuntimePackageDependency;
import static org.jboss.as.jaxrs.deployment.JaxrsDependencyProcessor.JACKSON_CORE_ASL;
import static org.jboss.as.jaxrs.deployment.JaxrsDependencyProcessor.JACKSON_DATATYPE_JDK8;
import static org.jboss.as.jaxrs.deployment.JaxrsDependencyProcessor.JACKSON_DATATYPE_JSR310;
import static org.jboss.as.jaxrs.deployment.JaxrsDependencyProcessor.JAXB_API;
import static org.jboss.as.jaxrs.deployment.JaxrsDependencyProcessor.JAXRS_API;
import static org.jboss.as.jaxrs.deployment.JaxrsDependencyProcessor.JSON_API;
import static org.jboss.as.jaxrs.deployment.JaxrsDependencyProcessor.RESTEASY_ATOM;
import static org.jboss.as.jaxrs.deployment.JaxrsDependencyProcessor.RESTEASY_CDI;
import static org.jboss.as.jaxrs.deployment.JaxrsDependencyProcessor.RESTEASY_CRYPTO;
import static org.jboss.as.jaxrs.deployment.JaxrsDependencyProcessor.RESTEASY_JACKSON2;
import static org.jboss.as.jaxrs.deployment.JaxrsDependencyProcessor.RESTEASY_JAXB;
import static org.jboss.as.jaxrs.deployment.JaxrsDependencyProcessor.RESTEASY_JAXRS;
import static org.jboss.as.jaxrs.deployment.JaxrsDependencyProcessor.RESTEASY_JSAPI;
import static org.jboss.as.jaxrs.deployment.JaxrsDependencyProcessor.RESTEASY_JSON_B_PROVIDER;
import static org.jboss.as.jaxrs.deployment.JaxrsDependencyProcessor.RESTEASY_JSON_P_PROVIDER;
import static org.jboss.as.jaxrs.deployment.JaxrsDependencyProcessor.RESTEASY_MULTIPART;
import static org.jboss.as.jaxrs.deployment.JaxrsDependencyProcessor.RESTEASY_VALIDATOR_11;
import static org.jboss.as.jaxrs.deployment.JaxrsDependencyProcessor.RESTEASY_YAML;
/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2014 Red Hat, inc.
 */
public class JaxrsSubsystemDefinition extends SimpleResourceDefinition {

    public static final JaxrsSubsystemDefinition INSTANCE = new JaxrsSubsystemDefinition();

    private JaxrsSubsystemDefinition() {
         super(new Parameters(JaxrsExtension.SUBSYSTEM_PATH, JaxrsExtension.getResolver())
                 .setAddHandler(JaxrsSubsystemAdd.INSTANCE)
                 .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE));
    }

    @Override
    public void registerAdditionalPackages(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerAdditionalPackages(RuntimePackageDependency.passive(RESTEASY_CDI.getName()),
                    RuntimePackageDependency.passive(RESTEASY_VALIDATOR_11.getName()),
                    RuntimePackageDependency.required(JAXRS_API.getName()),
                    RuntimePackageDependency.required(JAXB_API.getName()),
                    RuntimePackageDependency.required(JSON_API.getName()),
                    RuntimePackageDependency.optional(RESTEASY_ATOM.getName()),
                    RuntimePackageDependency.optional(RESTEASY_JAXRS.getName()),
                    RuntimePackageDependency.optional(RESTEASY_JAXB.getName()),
                    RuntimePackageDependency.optional(RESTEASY_JACKSON2.getName()),
                    RuntimePackageDependency.optional(RESTEASY_JSON_P_PROVIDER.getName()),
                    RuntimePackageDependency.optional(RESTEASY_JSON_B_PROVIDER.getName()),
                    RuntimePackageDependency.optional(RESTEASY_JSAPI.getName()),
                    RuntimePackageDependency.optional(RESTEASY_MULTIPART.getName()),
                    RuntimePackageDependency.optional(RESTEASY_YAML.getName()),
                    RuntimePackageDependency.optional(JACKSON_CORE_ASL.getName()),
                    RuntimePackageDependency.optional(RESTEASY_CRYPTO.getName()),
                    RuntimePackageDependency.optional(JACKSON_DATATYPE_JDK8.getName()),
                    RuntimePackageDependency.optional(JACKSON_DATATYPE_JSR310.getName()),
                    RuntimePackageDependency.optional("org.jboss.resteasy.resteasy-jettison-provider"),
                    RuntimePackageDependency.optional("org.jboss.resteasy.resteasy-jackson-provider"),
                    RuntimePackageDependency.optional("org.jboss.resteasy.resteasy-spring"));
    }
}
