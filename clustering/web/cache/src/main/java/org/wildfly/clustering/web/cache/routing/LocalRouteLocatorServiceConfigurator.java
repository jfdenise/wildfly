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

package org.wildfly.clustering.web.cache.routing;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.web.WebDeploymentConfiguration;
import org.wildfly.clustering.web.WebDeploymentRequirement;
import org.wildfly.clustering.web.routing.RouteLocator;
import org.wildfly.clustering.web.routing.RoutingStrategy;
import org.wildfly.clustering.web.session.DistributableSessionManagementConfiguration;

/**
 * Service configurator for the local routing provider.
 * @author Paul Ferraro
 */
public class LocalRouteLocatorServiceConfigurator extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, LocalRouteLocatorConfiguration {

    private final RoutingStrategy strategy;
    private final SupplierDependency<String> route;

    public LocalRouteLocatorServiceConfigurator(DistributableSessionManagementConfiguration managementConfiguration, WebDeploymentConfiguration deploymentConfiguration) {
        super(ServiceNameFactory.parseServiceName(WebDeploymentRequirement.ROUTE_LOCATOR.resolve(deploymentConfiguration.getServerName())));
        this.route = new ServiceSupplierDependency<>(ServiceNameFactory.parseServiceName(WebDeploymentRequirement.LOCAL_ROUTE.resolve(deploymentConfiguration.getServerName())));
        this.strategy = managementConfiguration.getRoutingStrategy();
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<RouteLocator> locator = this.route.register(builder).provides(this.getServiceName());
        return builder.setInstance(Service.newInstance(locator, new LocalRouteLocator(this))).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public RoutingStrategy getRoutingStrategy() {
        return this.strategy;
    }

    @Override
    public Supplier<String> getRoute() {
        return this.route;
    }
}
