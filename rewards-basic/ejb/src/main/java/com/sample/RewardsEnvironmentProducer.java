package com.sample;

import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceUnit;

import org.jbpm.runtime.manager.impl.RuntimeEnvironmentBuilder;
import org.jbpm.services.task.identity.JBossUserGroupCallbackImpl;
import org.kie.api.io.ResourceType;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.runtime.manager.RuntimeEnvironment;
import org.kie.internal.runtime.manager.cdi.qualifier.PerProcessInstance;
import org.kie.internal.runtime.manager.cdi.qualifier.PerRequest;
import org.kie.internal.runtime.manager.cdi.qualifier.Singleton;

@ApplicationScoped
public class RewardsEnvironmentProducer {

    private EntityManagerFactory emf;
    
    @PersistenceUnit(unitName = "org.jbpm.persistence.jpa")
    @ApplicationScoped
    @Produces
    public EntityManagerFactory getEntityManagerFactory() {
        if (this.emf == null) {
            // this needs to be here for non EE containers
            this.emf = Persistence.createEntityManagerFactory("org.jbpm.domain");

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
}
