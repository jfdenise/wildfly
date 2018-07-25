/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.clustering.web.undertow.routing;

import java.util.Iterator;
import java.util.ServiceLoader;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.dmr.ModelNode;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.service.ActiveServiceSupplier;
import org.wildfly.clustering.service.FunctionSupplierDependency;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.web.routing.RoutingProvider;
import org.wildfly.clustering.web.undertow.UndertowUnaryRequirement;
import org.wildfly.clustering.web.WebRequirement;
import org.wildfly.clustering.web.routing.LegacyRoutingProviderFactory;
import org.wildfly.extension.undertow.Server;
import org.wildfly.extension.undertow.session.DistributableServerRuntimeHandler;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(DistributableServerRuntimeHandler.class)
public class UndertowDistributableServerRuntimeHandler implements DistributableServerRuntimeHandler {
    @Override
    public void execute(OperationContext context, String serverName) {
        if (context.hasOptionalCapability(WebRequirement.ROUTING_PROVIDER.getName(), UndertowUnaryRequirement.SERVER.resolve(serverName), null)) {
            OperationStepHandler handler = new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    RoutingProvider provider = new ActiveServiceSupplier<RoutingProvider>(context.getServiceRegistry(true), WebRequirement.ROUTING_PROVIDER.getServiceName(context)).get();
                    installServerDependencies(context, provider, serverName);
                }
            };
            context.addStep(handler, OperationContext.Stage.VERIFY); // Run handler after RUNTIME stage, to ensure RoutingProvider service is installed
        } else {
            // Use legacy routing provider
            Iterator<LegacyRoutingProviderFactory> factories = ServiceLoader.load(LegacyRoutingProviderFactory.class, LegacyRoutingProviderFactory.class.getClassLoader()).iterator();
            if (factories.hasNext()) {
                installServerDependencies(context, factories.next().createRoutingProvider(), serverName);
            }
        }
    }

    static void installServerDependencies(OperationContext context, RoutingProvider provider, String serverName) {
        SupplierDependency<Server> server = new ServiceSupplierDependency<>(UndertowUnaryRequirement.SERVER.getServiceName(context, serverName));
        SupplierDependency<String> route = new FunctionSupplierDependency<>(server, Server::getRoute);
        for (CapabilityServiceConfigurator configurator : provider.getServiceConfigurators(serverName, route)) {
            configurator.configure(context).build(context.getServiceTarget()).install();
        }
    }
}
