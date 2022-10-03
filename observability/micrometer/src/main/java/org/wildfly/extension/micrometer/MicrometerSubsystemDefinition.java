/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extension.micrometer;

import java.util.Arrays;
import java.util.Collection;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.RuntimePackageDependency;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.micrometer.metrics.MicrometerCollector;
import org.wildfly.extension.micrometer.metrics.WildFlyRegistry;

public class MicrometerSubsystemDefinition extends PersistentResourceDefinition {
    public static final String MICROMETER_MODULE = "org.wildfly.extension.micrometer";
    static final String CLIENT_FACTORY_CAPABILITY = "org.wildfly.management.model-controller-client-factory";
    static final String HTTP_EXTENSIBILITY_CAPABILITY = "org.wildfly.management.http.extensible";
    static final String MANAGEMENT_EXECUTOR = "org.wildfly.management.executor";
    static final String METRICS_SCAN_CAPABILITY = "org.wildfly.extension.metrics.scan";
    static final String MICROMETER_HTTP_SECURITY_CAPABILITY = MICROMETER_MODULE + ".http-context.security-enabled";
    static final String PROCESS_STATE_NOTIFIER = "org.wildfly.management.process-state-notifier";
    static final String METRICS_HTTP_CONTEXT_CAPABILITY = "org.wildfly.extension.metrics.http-context";

    private static final RuntimeCapability<Void> MICROMETER_COLLECTOR_RUNTIME_CAPABILITY =
            RuntimeCapability.Builder.of(MICROMETER_MODULE + ".wildfly-collector", MicrometerCollector.class)
                    .addRequirements(CLIENT_FACTORY_CAPABILITY, MANAGEMENT_EXECUTOR, PROCESS_STATE_NOTIFIER)
                    .build();
    static final RuntimeCapability<Void> MICROMETER_REGISTRY_RUNTIME_CAPABILITY =
            RuntimeCapability.Builder.of(MICROMETER_MODULE + ".registry", WildFlyRegistry.class)
                    .build();
    static final RuntimeCapability<Void> MICROMETER_HTTP_CONTEXT_CAPABILITY =
            RuntimeCapability.Builder.of(MICROMETER_MODULE + ".http-context", MicrometerContextService.class)
                    .addRequirements(METRICS_HTTP_CONTEXT_CAPABILITY)
                    .build();

    public static final ServiceName MICROMETER_COLLECTOR = MICROMETER_COLLECTOR_RUNTIME_CAPABILITY.getCapabilityServiceName();

    public static final String[] MODULES = {
            "io.micrometer",
            "io.prometheus",
            "org.latencyutils"
    };

    public static final String[] EXPORTED_MODULES = {
            "io.micrometer"
    };

    static final AttributeDefinition SECURITY_ENABLED =
            SimpleAttributeDefinitionBuilder.create("security-enabled", ModelType.BOOLEAN)
                    .setDefaultValue(ModelNode.FALSE)
                    .setRequired(false)
                    .setRestartAllServices()
                    .setAllowExpression(true)
                    .build();

    static final StringListAttributeDefinition EXPOSED_SUBSYSTEMS =
            new StringListAttributeDefinition.Builder("exposed-subsystems" )
                    .setRequired(false)
                    .setRestartAllServices()
                    .build();

    static final AttributeDefinition[] ATTRIBUTES = {SECURITY_ENABLED, EXPOSED_SUBSYSTEMS};
    static final MicrometerSubsystemDefinition INSTANCE = new MicrometerSubsystemDefinition();

    protected MicrometerSubsystemDefinition() {
        super(new SimpleResourceDefinition.Parameters(MicrometerSubsystemExtension.SUBSYSTEM_PATH,
                MicrometerSubsystemExtension.getResourceDescriptionResolver())
                .setAddHandler(MicrometerSubsystemAdd.INSTANCE)
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
                .setCapabilities(MICROMETER_HTTP_CONTEXT_CAPABILITY));
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }

    @Override
    public void registerAdditionalRuntimePackages(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerAdditionalRuntimePackages(
                RuntimePackageDependency.required("io.micrometer" ),
                RuntimePackageDependency.required("io.prometheus" ),
                RuntimePackageDependency.required("org.latencyutils" )
        );
    }
}
