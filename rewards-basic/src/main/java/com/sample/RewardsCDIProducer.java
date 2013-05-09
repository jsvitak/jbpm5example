package com.sample;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.droolsjbpm.services.api.IdentityProvider;
import org.jbpm.runtime.manager.impl.RuntimeEnvironmentBuilder;
import org.jbpm.services.task.identity.JBossUserGroupCallbackImpl;
import org.kie.api.io.ResourceType;
import org.kie.commons.io.IOService;
import org.kie.commons.io.impl.IOServiceNio2WrapperImpl;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.runtime.manager.RuntimeEnvironment;
import org.kie.internal.runtime.manager.cdi.qualifier.PerProcessInstance;
import org.kie.internal.runtime.manager.cdi.qualifier.PerRequest;
import org.kie.internal.runtime.manager.cdi.qualifier.Singleton;
import org.kie.internal.task.api.UserGroupCallback;

@ApplicationScoped
public class RewardsCDIProducer {

    private EntityManagerFactory emf;

    @Produces
    public EntityManagerFactory produceEntityManagerFactory() {
        if (this.emf == null) {
            this.emf = Persistence
                    .createEntityManagerFactory("org.jbpm.persistence.jpa");

        }
        return this.emf;
    }

    @Produces
    @Singleton
    @PerProcessInstance
    @PerRequest
    public RuntimeEnvironment produceEnvironment(EntityManagerFactory emf) {
        Properties properties = new Properties();
        RuntimeEnvironment environment = RuntimeEnvironmentBuilder
                .getDefault()
                .entityManagerFactory(emf)
                .userGroupCallback(new JBossUserGroupCallbackImpl(properties))
                .addAsset(
                        ResourceFactory
                                .newClassPathResource("rewards-basic.bpmn"),
                        ResourceType.BPMN2).get();
        return environment;
    }

    @Produces
    @ApplicationScoped
    public EntityManager getEntityManager() {
        EntityManager em = produceEntityManagerFactory().createEntityManager();
        em.getTransaction().begin();
        return em;
    }

    @ApplicationScoped
    public void commitAndClose(@Disposes EntityManager em) {
        try {
            em.getTransaction().commit();
            em.close();
        } catch (Exception e) {

        }
    }

    @Produces
    public Logger createLogger(InjectionPoint injectionPoint) {
        return Logger.getLogger(injectionPoint.getMember().getDeclaringClass()
                .getName());
    }

    @Produces
    @Named("ioStrategy")
    public IOService createIOService() {
        return new IOServiceNio2WrapperImpl();
    }

    @Produces
    public UserGroupCallback produceUserGroupCallback() {
        return new RewardsUserGroupCallback();
    }
    
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
    
    @Produces
    public IdentityProvider produceIdentityProvider() {
        return new RewardsIdentityProvider();
    }
    
    private class RewardsIdentityProvider implements IdentityProvider {

        public String getName() {
            return "testUser";
        }

        public List<String> getRoles() {
            return Collections.emptyList();
        }

    }

}
