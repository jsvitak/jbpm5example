package com.sample;

import java.util.List;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.persistence.OptimisticLockException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

import org.jbpm.runtime.manager.impl.RuntimeEnvironmentBuilder;
import org.jbpm.services.task.exception.PermissionDeniedException;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.runtime.manager.RuntimeManagerFactory;
import org.kie.internal.runtime.manager.context.EmptyContext;



@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class TaskBean implements TaskLocal {

    @Inject
    private RuntimeManagerFactory managerFactory;
    
    @Resource
    private UserTransaction ut;

    public List<TaskSummary> retrieveTaskList(String actorId) throws Exception {

        RuntimeEnvironmentBuilder builder = RuntimeEnvironmentBuilder.getDefault();
        RuntimeManager manager = managerFactory.newSingletonRuntimeManager(builder.get());
        RuntimeEngine runtime = manager.getRuntimeEngine(EmptyContext.get());
        TaskService taskService = runtime.getTaskService();

        List<TaskSummary> list = taskService.getTasksAssignedAsPotentialOwner(actorId, "en-UK");

        System.out.println("retrieveTaskList by " + actorId);
        for (TaskSummary task : list) {
            System.out.println(" task.getId() = " + task.getId());
        }

        return list;
    }

    public void approveTask(String actorId, long taskId) throws Exception {

        RuntimeEnvironmentBuilder builder = RuntimeEnvironmentBuilder.getDefault();
        RuntimeManager manager = managerFactory.newSingletonRuntimeManager(builder.get());
        RuntimeEngine runtime = manager.getRuntimeEngine(EmptyContext.get());
        TaskService taskService = runtime.getTaskService();

        ut.begin();

        try {
            System.out.println("approveTask (taskId = " + taskId + ") by " + actorId);
            taskService.start(taskId, actorId);
            taskService.complete(taskId, actorId, null);

            //Thread.sleep(10000); // To test OptimisticLockException

            ut.commit();
        } catch (RollbackException e) {
            e.printStackTrace();
            Throwable cause = e.getCause();
            if (cause != null && cause instanceof OptimisticLockException) {
                // Concurrent access to the same process instance
                throw new ProcessOperationException("The same process instance has likely been accessed concurrently",
                        e);
            }
            throw new RuntimeException(e);
        } catch (PermissionDeniedException e) {
            e.printStackTrace();
            // Transaction might be already rolled back by TaskServiceSession
            if (ut.getStatus() == Status.STATUS_ACTIVE) {
                ut.rollback();
            }
            // Probably the task has already been started by other users
            throw new ProcessOperationException("The task (id = " + taskId
                    + ") has likely been started by other users ", e);
        } catch (Exception e) {
            e.printStackTrace();
            // Transaction might be already rolled back by TaskServiceSession
            if (ut.getStatus() == Status.STATUS_ACTIVE) {
                ut.rollback();
            }
            throw new RuntimeException(e);
        } finally {

        }
    }

    
    
//    private static KnowledgeBase kbase;
//
//    @PersistenceUnit(unitName = "org.jbpm.persistence.jpa")
//    private EntityManagerFactory emf;
//
//    @Resource
//    private UserTransaction ut;
//
//    public List<TaskSummary> retrieveTaskList(String actorId) throws Exception {
//
//        kbase = readKnowledgeBase();
//
//        StatefulKnowledgeSession ksession = createKnowledgeSession();
//        TaskService localTaskService = getTaskService(ksession);
//
//        List<TaskSummary> list = localTaskService.getTasksAssignedAsPotentialOwner(actorId, "en-UK");
//
//        System.out.println("retrieveTaskList by " + actorId);
//        for (TaskSummary task : list) {
//            System.out.println(" task.getId() = " + task.getId());
//        }
//
//        ksession.dispose();
//
//        return list;
//    }
//
//    public void approveTask(String actorId, long taskId) throws Exception {
//
//        kbase = readKnowledgeBase();
//
//        StatefulKnowledgeSession ksession = createKnowledgeSession();
//        TaskService localTaskService = getTaskService(ksession);
//
//        ut.begin();
//
//        try {
//            System.out.println("approveTask (taskId = " + taskId + ") by " + actorId);
//            localTaskService.start(taskId, actorId);
//            localTaskService.complete(taskId, actorId, null);
//
//            //Thread.sleep(10000); // To test OptimisticLockException
//
//            ut.commit();
//        } catch (RollbackException e) {
//            e.printStackTrace();
//            Throwable cause = e.getCause();
//            if (cause != null && cause instanceof OptimisticLockException) {
//                // Concurrent access to the same process instance
//                throw new ProcessOperationException("The same process instance has likely been accessed concurrently",
//                        e);
//            }
//            throw new RuntimeException(e);
//        } catch (PermissionDeniedException e) {
//            e.printStackTrace();
//            // Transaction might be already rolled back by TaskServiceSession
//            if (ut.getStatus() == Status.STATUS_ACTIVE) {
//                ut.rollback();
//            }
//            // Probably the task has already been started by other users
//            throw new ProcessOperationException("The task (id = " + taskId
//                    + ") has likely been started by other users ", e);
//        } catch (Exception e) {
//            e.printStackTrace();
//            // Transaction might be already rolled back by TaskServiceSession
//            if (ut.getStatus() == Status.STATUS_ACTIVE) {
//                ut.rollback();
//            }
//            throw new RuntimeException(e);
//        } finally {
//            ksession.dispose();
//        }
//
//        return;
//    }
//
//    private StatefulKnowledgeSession createKnowledgeSession() {
//        Environment env = KnowledgeBaseFactory.newEnvironment();
//        env.set(EnvironmentName.ENTITY_MANAGER_FACTORY, emf);
//
//        StatefulKnowledgeSession ksession = JPAKnowledgeService.newStatefulKnowledgeSession(kbase, null, env);
//
//        new JPAWorkingMemoryDbLogger(ksession);
//
//        return ksession;
//    }
//
//    private TaskService getTaskService(StatefulKnowledgeSession ksession) {
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
//        return localTaskService;
//    }
//
//    private KnowledgeBase readKnowledgeBase() throws Exception {
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
