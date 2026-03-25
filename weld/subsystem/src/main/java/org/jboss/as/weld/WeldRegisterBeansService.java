/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.jboss.as.server.deployment.SetupAction;
import org.jboss.as.weld.logging.WeldLogger;

import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.weld.bean.ContextualInstance;
import org.jboss.weld.bean.builtin.BeanManagerProxy;
import org.wildfly.graal.runtime.WildFlyGraalSetup;
import org.wildfly.security.manager.WildFlySecurityManager;

public class WeldRegisterBeansService implements Service {

    public static final ServiceName SERVICE_NAME = ServiceNames.WELD_REGISTER_BEANS_SERVICE_NAME;

    private final Supplier<BeanManager> beanManagerSupplier;
    private final ClassLoader classLoader;
    private final List<SetupAction> setupActions;
    private final AtomicBoolean runOnce = new AtomicBoolean();

    public WeldRegisterBeansService(final Supplier<BeanManager> beanManagerSupplier,
                                    final List<SetupAction> setupActions,
                                    final ClassLoader classLoader) {
        this.beanManagerSupplier = beanManagerSupplier;
        this.setupActions = setupActions;
        this.classLoader = classLoader;
    }

    @Override
    public void start(final StartContext context) throws StartException {
        if (!runOnce.compareAndSet(false, true)) {
            // we only execute this once, if there is a restart, WeldStartService initiates re-deploy hence we do nothing here
            return;
        }
        ClassLoader oldTccl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            for (SetupAction action : setupActions) {
                action.setup(null);
            }
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
            BeanManager beanManager = beanManagerSupplier.get();
            Class[] classes = WildFlyGraalSetup.getCDIClasses();
            for (Class clazz : classes) {
                System.out.println("Force Proxies creation for " + clazz.getName());
                Bean<?> bean = beanManager.resolve(beanManager.getBeans(clazz));
                CreationalContext<?> creationalContext = beanManager.createCreationalContext(bean);
                beanManager.getReference(bean, clazz, creationalContext);
                try {
                    Object obj = ContextualInstance.get(bean, ((BeanManagerProxy)beanManager).delegate(), creationalContext);
                } catch(Exception ex) {
                    // OK, attempt to create an instance that can be invalid for transient scopes (e.g.: request).
                    System.err.println(ex);
                    //ex.printStackTrace();
                }
            }
        } catch(Exception ex) {
            throw new StartException(ex);
        } finally {
            for (SetupAction action : setupActions) {try {
                action.teardown(null);
            } catch (Exception e) {
                WeldLogger.DEPLOYMENT_LOGGER.exceptionClearingThreadState(e);
            }
            }
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(oldTccl);
        }
    }

    @Override
    public void stop(StopContext context) {
        // No-op
    }

}
