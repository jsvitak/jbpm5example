package com.sample;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceUnit;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

import org.droolsjbpm.services.api.IdentityProvider;
import org.jbpm.runtime.manager.impl.RuntimeEnvironmentBuilder;
import org.jbpm.services.task.identity.JBossUserGroupCallbackImpl;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.commons.io.IOService;
import org.kie.commons.io.impl.IOServiceNio2WrapperImpl;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.runtime.manager.RuntimeEnvironment;
import org.kie.internal.runtime.manager.RuntimeManagerFactory;
import org.kie.internal.runtime.manager.cdi.qualifier.PerProcessInstance;
import org.kie.internal.runtime.manager.cdi.qualifier.PerRequest;
import org.kie.internal.runtime.manager.cdi.qualifier.Singleton;
import org.kie.internal.task.api.UserGroupCallback;

@ApplicationScoped
public class RewardsEnvironmentProducer {

    private EntityManagerFactory emf;
    private IOService ioService = new IOServiceNio2WrapperImpl();
    
    @Produces
    public UserGroupCallback produceSelectedUserGroupCalback() {
        return new RewardsUserGroupCallback();
    }
    
    @Produces
    public IdentityProvider produceIdentityProvider() {
        return new RewardsIdentityProvider();
    }
    
    @PersistenceUnit(unitName = "org.jbpm.persistence.jpa")
    @ApplicationScoped
    @Produces
    public EntityManagerFactory getEntityManagerFactory() {
        if (this.emf == null) {
            // this needs to be here for non EE containers
            this.emf = Persistence.createEntityManagerFactory("org.jbpm.persistence.jpa");

        }
        return this.emf;
    }
    
    @Produces
    @Singleton
    @PerRequest
    @PerProcessInstance
    @RewardsRuntimeEnvironment
    public RuntimeEnvironment produceEnvironment(EntityManagerFactory emf) {
        Properties properties= new Properties();
        RuntimeEnvironment environment = RuntimeEnvironmentBuilder.getDefault()
                .entityManagerFactory(emf).userGroupCallback(new JBossUserGroupCallbackImpl(properties))
                .addAsset(ResourceFactory.newClassPathResource("rewards-basic.bpmn"), ResourceType.BPMN2)
                .get();
        return environment;
    }
    
//    results in NPE of RuntimeEngine, manager is injected fine, but getRuntimeEngine returns null
//    
//    @Inject
//    RuntimeManagerFactory managerFactory;
//    
//    @Inject
//    @RewardsRuntimeEnvironment
//    RuntimeEnvironment runtimeEnvironment;
//    
//    @Produces
//    @Singleton
//    @PerRequest
//    @PerProcessInstance
//    public RuntimeManager produceRuntimeManager() {
//        return managerFactory.newSingletonRuntimeManager(runtimeEnvironment);
//    }
    
    
    @Produces
    @ApplicationScoped
    public EntityManager getEntityManager() {
        final EntityManager em = getEntityManagerFactory().createEntityManager();
        EntityManager emProxy = (EntityManager)
                Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{EntityManager.class}, new EmInvocationHandler(em));
        return emProxy;
    }
    
    @ApplicationScoped
    public void commitAndClose(@Disposes EntityManager em) {
        try {
            
            em.close();
        } catch (Exception e) {

        }
    }

    @Produces
    public Logger createLogger(InjectionPoint injectionPoint) {
        return Logger.getLogger(injectionPoint.getMember()
                .getDeclaringClass().getName());
    }
    
    @Produces
    @Named("ioStrategy")
    public IOService prepareFileSystem() {

        return ioService;
    }
    
    private class EmInvocationHandler implements InvocationHandler {

        private EntityManager delegate;
        
        EmInvocationHandler(EntityManager em) {
            this.delegate = em;
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            joinTransactionIfNeeded();
            return method.invoke(delegate, args);
        }
        
        private void joinTransactionIfNeeded() {
            try {
                UserTransaction ut = InitialContext.doLookup("java:comp/UserTransaction");
                if (ut.getStatus() == Status.STATUS_ACTIVE) {
                    delegate.joinTransaction();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
    }
    
    // in 6.0.0.Beta2 the provided callbacks seem to be broken, implementing own simple one
    private class RewardsUserGroupCallback implements UserGroupCallback {

        public boolean existsUser(String userId) {
            return userId.equals("john") || userId.equals("mary");
        }

        public boolean existsGroup(String groupId) {
            return groupId.equals("PM") || groupId.equals("HR");
        }

        public List<String> getGroupsForUser(String userId,
                List<String> groupIds, List<String> allExistingGroupIds) {
            List<String> groups = new ArrayList<String>();
            if (userId.equals("john"))
                groups.add("PM");
            else if (userId.equals("mary"))
                groups.add("HR");
            return groups;
        }
        
    }
    
    
    private class RewardsIdentityProvider implements IdentityProvider {

        public String getName() {
            return "testUser";
        }

        public List<String> getRoles() {
            return Collections.EMPTY_LIST;
        }

    }
}
