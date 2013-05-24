package com.sample.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceUnit;


import org.jbpm.runtime.manager.impl.RuntimeEnvironmentBuilder;
import org.jbpm.runtime.manager.impl.cdi.InjectableRegisterableItemsFactory;
import org.jbpm.services.task.identity.JBossUserGroupCallbackImpl;
import org.jbpm.services.task.wih.RuntimeFinder;
import org.kie.api.io.ResourceType;
import org.kie.commons.io.IOService;
import org.kie.commons.io.impl.IOServiceNio2WrapperImpl;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.runtime.manager.RegisterableItemsFactory;
import org.kie.internal.runtime.manager.RuntimeEnvironment;
import org.kie.internal.runtime.manager.cdi.qualifier.PerProcessInstance;
import org.kie.internal.runtime.manager.cdi.qualifier.PerRequest;
import org.kie.internal.runtime.manager.cdi.qualifier.Singleton;
import org.kie.internal.task.api.UserGroupCallback;

@ApplicationScoped
public class RewardsApplicationScopedProducer {

    @Inject
    private InjectableRegisterableItemsFactory factory;
    @Inject
    private UserGroupCallback usergroupCallback;

    @PersistenceUnit(unitName = "com.sample.rewards")
    private EntityManagerFactory emf;


    @Produces
    public UserGroupCallback produceUserGroupCallback() {
        return usergroupCallback;
    }

    @Produces
    public EntityManagerFactory produceEntityManagerFactory() {
        if (this.emf == null) {
            this.emf = Persistence
                    .createEntityManagerFactory("com.sample.rewards");
        }
        return this.emf;
    }

    @Produces
    @Singleton
    @PerProcessInstance
    @PerRequest
    public RuntimeEnvironment produceEnvironment(EntityManagerFactory emf) {
        RuntimeEnvironment environment = RuntimeEnvironmentBuilder
                .getDefault()
                .entityManagerFactory(emf)
                .userGroupCallback(usergroupCallback)
                .registerableItemsFactory(factory)
                .addAsset(
                        ResourceFactory
                                .newClassPathResource("rewards-basic.bpmn"),
                        ResourceType.BPMN2).get();
        return environment;
    }


}
