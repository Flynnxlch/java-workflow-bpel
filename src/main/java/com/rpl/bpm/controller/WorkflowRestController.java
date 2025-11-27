package com.rpl.bpm.controller;

import com.rpl.bpm.entity.User;
import com.rpl.bpm.repository.UserRepository;
import com.rpl.bpm.service.WorkflowService;
import com.rpl.bpm.service.WorkflowServiceSimple;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/workflow")
public class WorkflowRestController {

    @Autowired(required = false)
    private WorkflowService workflowService;
    
    @Autowired(required = false)
    private WorkflowServiceSimple workflowServiceSimple;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/start/{processKey}")
    public ResponseEntity<Map<String, Object>> startProcess(
            @PathVariable String processKey,
            @RequestBody(required = false) Map<String, Object> variables,
            Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // Only ADMIN can start workflow
        if (!user.getRole().equals("ADMIN")) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Only administrators can start workflows!");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        
        try {
            String processInstanceId;
            if (workflowService != null) {
                processInstanceId = workflowService.startProcess(processKey, variables, user);
            } else if (workflowServiceSimple != null) {
                processInstanceId = workflowServiceSimple.startProcess(processKey, variables, user);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Workflow service is not available");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Process started successfully");
            response.put("processInstanceId", processInstanceId);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error starting process: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/tasks")
    public ResponseEntity<List<Map<String, Object>>> getUserTasks(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        List<Map<String, Object>> taskList = new ArrayList<>();
        if (workflowService != null) {
            List<Task> tasks = workflowService.getUserTasks(user.getUsername());
            if (tasks != null) {
                taskList = tasks.stream()
                    .map(task -> {
                        Map<String, Object> taskMap = new HashMap<>();
                        taskMap.put("id", task.getId());
                        taskMap.put("name", task.getName());
                        taskMap.put("assignee", task.getAssignee());
                        taskMap.put("processInstanceId", task.getProcessInstanceId());
                        taskMap.put("createTime", task.getCreateTime());
                        return taskMap;
                    })
                    .collect(Collectors.toList());
            }
        } else if (workflowServiceSimple != null) {
            List<Map<String, Object>> tasks = workflowServiceSimple.getUserTasks(user.getUsername());
            if (tasks != null) {
                taskList = tasks;
            }
        }
        
        return ResponseEntity.ok(taskList);
    }

    @PostMapping("/tasks/{taskId}/complete")
    public ResponseEntity<Map<String, Object>> completeTask(
            @PathVariable String taskId,
            @RequestBody(required = false) Map<String, Object> variables,
            Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            if (workflowService != null) {
                workflowService.completeTask(taskId, variables);
            } else if (workflowServiceSimple != null) {
                workflowServiceSimple.completeTask(taskId, variables);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Workflow service is not available");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Task completed successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error completing task: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}

