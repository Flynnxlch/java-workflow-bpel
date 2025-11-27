package com.rpl.bpm.service;

import com.rpl.bpm.entity.ProcessResult;
import com.rpl.bpm.entity.User;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class WorkflowService {

    @Autowired(required = false)
    private RepositoryService repositoryService;

    @Autowired(required = false)
    private RuntimeService runtimeService;

    @Autowired(required = false)
    private TaskService taskService;

    @Autowired
    private ProcessResultService processResultService;

    public String startProcess(String processKey, Map<String, Object> variables, User user) {
        if (runtimeService == null) {
            throw new RuntimeException("Flowable RuntimeService is not available. BPMN engine is not initialized.");
        }
        
        if (variables == null) {
            variables = new HashMap<>();
        }
        variables.put("userId", user.getId().toString());
        variables.put("username", user.getUsername());
        
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processKey, variables);
        
        // Create process result record
        ProcessResult processResult = new ProcessResult();
        processResult.setProcessName(processKey);
        processResult.setProcessInstanceId(processInstance.getId());
        processResult.setStatus("PENDING");
        processResult.setResultData("Process started: " + processInstance.getId());
        processResult.setUser(user);
        processResultService.createProcessResult(processResult);
        
        return processInstance.getId();
    }

    public List<Task> getUserTasks(String username) {
        if (taskService == null) {
            return new java.util.ArrayList<>();
        }
        return taskService.createTaskQuery()
            .taskAssignee(username)
            .list();
    }

    public void completeTask(String taskId, Map<String, Object> variables) {
        if (taskService == null || runtimeService == null) {
            throw new RuntimeException("Flowable TaskService or RuntimeService is not available. BPMN engine is not initialized.");
        }
        if (variables == null) {
            variables = new HashMap<>();
        }
        taskService.complete(taskId, variables);
        
        // Update process result
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task != null) {
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .singleResult();
            
            if (processInstance != null) {
                List<ProcessResult> results = processResultService.getAllProcessResults();
                ProcessResult result = results.stream()
                    .filter(r -> r.getProcessInstanceId().equals(processInstance.getId()))
                    .findFirst()
                    .orElse(null);
                
                if (result != null) {
                    result.setStatus("COMPLETED");
                    result.setResultData("Task completed: " + task.getName());
                    processResultService.updateProcessResult(result.getId(), result);
                }
            }
        }
    }

    public List<ProcessInstance> getActiveProcessInstances() {
        return runtimeService.createProcessInstanceQuery()
            .active()
            .list();
    }

    public Map<String, Object> getProcessVariables(String processInstanceId) {
        return runtimeService.getVariables(processInstanceId);
    }

    public List<ProcessDefinition> getAvailableProcessDefinitions() {
        if (repositoryService == null) {
            return new java.util.ArrayList<>();
        }
        return repositoryService.createProcessDefinitionQuery()
            .latestVersion()
            .active()
            .list();
    }

    public ProcessDefinition getProcessDefinitionByKey(String processKey) {
        if (repositoryService == null) {
            return null;
        }
        return repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey(processKey)
            .latestVersion()
            .singleResult();
    }
    
    public boolean isFlowableAvailable() {
        return runtimeService != null && repositoryService != null && taskService != null;
    }
    
    public TaskService getTaskService() {
        return taskService;
    }
    
    public RuntimeService getRuntimeService() {
        return runtimeService;
    }
    
    public RepositoryService getRepositoryService() {
        return repositoryService;
    }
}

