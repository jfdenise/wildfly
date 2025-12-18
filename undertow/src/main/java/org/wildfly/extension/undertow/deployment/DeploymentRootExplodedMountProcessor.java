/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.deployment;

import io.undertow.servlet.api.AnnotationRetriever;
import io.undertow.servlet.util.DefaultAnnotationRetriever;
import java.util.Locale;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.MountExplodedMarker;

/**
 * Processor that marks a deployment as exploded.
 *
 * @author Thomas.Diesler@jboss.com
 * @since  05-Oct-2011
 */
public class DeploymentRootExplodedMountProcessor implements DeploymentUnitProcessor {

    private static final String WAR_EXTENSION = ".war";

    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        String depName = depUnit.getName().toLowerCase(Locale.ENGLISH);
        if (depName.endsWith(WAR_EXTENSION)) {
            MountExplodedMarker.setMountExploded(depUnit);
        }
        AnnotationRetriever retriever = DefaultAnnotationRetriever.INSTANCE;
        if(Boolean.getBoolean("org.wildfly.graal")) {
            retriever = GraalAnnotationRetriever.INSTANCE;
        }
        System.out.println("INSTALL ANNOTATION RETRIEVER " + retriever);
        phaseContext.getDeploymentUnit().putAttachment(UndertowAttachments.ANNOTATION_RETRIEVER, retriever);
    }
}
