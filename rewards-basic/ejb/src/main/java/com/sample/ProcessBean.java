package com.sample;


import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.inject.Qualifier;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

import org.jbpm.runtime.manager.impl.RuntimeEnvironmentBuilder;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.runtime.manager.RuntimeEnvironment;
import org.kie.internal.runtime.manager.RuntimeManagerFactory;
import org.kie.internal.runtime.manager.context.EmptyContext;

@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class ProcessBean implements ProcessLocal {

    //@PersistenceUnit(unitName = "org.jbpm.persistence.jpa")
    //private EntityManagerFactory emf;

    @Resource
    private UserTransaction ut;

    @Inject
    private RuntimeManagerFactory managerFactory;
    
    //@Inject
    //private BeanManager beanManager;
    
    //@Inject
    //private RuntimeEnvironment runtimeEnvironment;
    
    public long startProcess(String recipient) throws Exception {
     
//        RuntimeEnvironmentBuilder builder = RuntimeEnvironmentBuilder.getDefault()
//                .entityManagerFactory(emf)
//                .registerableItemsFactory(InjectableRegisterableItemsFactory.getFactory(beanManager, null));
        RuntimeEnvironmentBuilder builder = RuntimeEnvironmentBuilder.getDefault();
        
        builder.addAsset(ResourceFactory.newClassPathResource("rewards-basic.bpmn"), ResourceType.BPMN2);
        RuntimeManager manager = managerFactory.newSingletonRuntimeManager(builder.get());
        RuntimeEngine runtime = manager.getRuntimeEngine(EmptyContext.get());
        KieSession ksession = runtime.getKieSession();
        //TaskService taskService = runtime.getTaskService();
        
        long processInstanceId = -1;
        
        ut.begin();

        try {
            // start a new process instance
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("recipient", recipient);
            ProcessInstance processInstance = ksession.startProcess("com.sample.rewards-basic", params);

            processInstanceId = processInstance.getId();

            System.out.println("Process started ... : processInstanceId = " + processInstanceId);

            ut.commit();
        } catch (Exception e) {
            e.printStackTrace();
            if (ut.getStatus() == Status.STATUS_ACTIVE) {
                ut.rollback();
            }
            throw e;
        } finally {
            ksession.dispose();
        }

        return processInstanceId;
    }
    
     
//    private static KieBase kbase;
//
//    @PersistenceUnit(unitName = "org.jbpm.persistence.jpa")
//    private EntityManagerFactory emf;
//
//    @Resource
//    private UserTransaction ut;
//
//    public long startProcess(String recipient) throws Exception {
//
//        // Use this when you want to ignore user existence issues
//        UserGroupCallbackManager.getInstance().setCallback(new DefaultUserGroupCallbackImpl());
//
//        // load up the knowledge base
//        kbase = readKieBase();
//
//        KieSession ksession = createKieSession();
//
//        long processInstanceId = -1;
//
//        ut.begin();
//
//        try {
//            // start a new process instance
//            Map<String, Object> params = new HashMap<String, Object>();
//            params.put("recipient", recipient);
//            ProcessInstance processInstance = ksession.startProcess("com.sample.rewards-basic", params);
//
//            processInstanceId = processInstance.getId();
//
//            System.out.println("Process started ... : processInstanceId = " + processInstanceId);
//
//            ut.commit();
//        } catch (Exception e) {
//            e.printStackTrace();
//            if (ut.getStatus() == Status.STATUS_ACTIVE) {
//                ut.rollback();
//            }
//            throw e;
//        } finally {
//            ksession.dispose();
//        }
//
//        return processInstanceId;
//    }
//
//    private KieSession createKieSession() {
//        Environment env = KnowledgeBaseFactory.newEnvironment();
//        env.set(EnvironmentName.ENTITY_MANAGER_FACTORY, emf);
//
//        StatefulKnowledgeSession ksession = JPAKnowledgeService.newStatefulKnowledgeSession(kbase, null, env);
//
//        new JPAWorkingMemoryDbLogger(ksession);
//
//        org.jbpm.task.service.TaskService taskService = new org.jbpm.task.service.TaskService(emf,
//                SystemEventListenerFactory.getSystemEventListener());
//
//        LocalTaskService localTaskService = new LocalTaskService(taskService);
//
//        SyncWSHumanTaskHandler humanTaskHandler = new SyncWSHumanTaskHandler(localTaskService, ksession);
//        humanTaskHandler.setLocal(true);
//        humanTaskHandler.connect();
//        ksession.getWorkItemManager().registerWorkItemHandler("Human Task", humanTaskHandler);
//
//        return ksession;
//    }
//
//    private static KieBase readKieBase() throws Exception {
//
//        if (kbase != null) {
//            return kbase;
//        }
//
//        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
//        kbuilder.add(ResourceFactory.newClassPathResource("rewards-basic.bpmn"), ResourceType.BPMN2);
//        return kbuilder.newKnowledgeBase();
//    }

}
