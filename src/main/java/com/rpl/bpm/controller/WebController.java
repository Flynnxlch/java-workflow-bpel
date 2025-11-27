package com.rpl.bpm.controller;

import com.rpl.bpm.entity.ProcessResult;
import com.rpl.bpm.entity.TaskAssignment;
import com.rpl.bpm.entity.User;
import com.rpl.bpm.repository.UserRepository;
import com.rpl.bpm.entity.WorkflowAssignment;
import com.rpl.bpm.service.ProcessResultService;
import com.rpl.bpm.service.TaskAssignmentService;
import com.rpl.bpm.service.UserService;
import com.rpl.bpm.service.WorkflowAssignmentService;
import com.rpl.bpm.service.WorkflowService;
import com.rpl.bpm.service.WorkflowServiceSimple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class WebController {

    @Autowired
    private UserService userService;

    @Autowired
    private ProcessResultService processResultService;

    @Autowired(required = false)
    private WorkflowService workflowService;
    
    @Autowired(required = false)
    private WorkflowServiceSimple workflowServiceSimple;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskAssignmentService taskAssignmentService;

    @Autowired
    private WorkflowAssignmentService workflowAssignmentService;

    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }

    @GetMapping("/error")
    public String error() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                       @RequestParam(required = false) String logout,
                       Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password!");
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully.");
        }
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute User user, RedirectAttributes redirectAttributes) {
        try {
            userService.createUser(user);
            redirectAttributes.addFlashAttribute("message", "Registration successful! Please login.");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Registration failed: " + e.getMessage());
            return "redirect:/register";
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }
        
        List<ProcessResult> processResults = processResultService.getProcessResultsByUser(user);
        
        // Try to get tasks, but handle if workflow service is not available
        try {
            if (workflowService != null) {
                // Get tasks assigned to this user
                List<?> tasks = workflowService.getUserTasks(user.getUsername());
                model.addAttribute("tasks", tasks != null ? tasks : new java.util.ArrayList<>());
            } else if (workflowServiceSimple != null) {
                List<?> tasks = workflowServiceSimple.getUserTasks(user.getUsername());
                model.addAttribute("tasks", tasks != null ? tasks : new java.util.ArrayList<>());
            } else {
                model.addAttribute("tasks", new java.util.ArrayList<>());
            }
        } catch (Exception e) {
            System.err.println("Error getting tasks: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("tasks", new java.util.ArrayList<>());
        }
        
        // Get task assignments for user
        List<TaskAssignment> taskAssignments = taskAssignmentService.getPendingTaskAssignmentsByUser(user);
        model.addAttribute("taskAssignments", taskAssignments);
        
        // Get workflows assigned to user (using WorkflowAssignment)
        List<WorkflowAssignment> userWorkflowAssignments = workflowAssignmentService.getWorkflowAssignmentsByUser(user);
        List<ProcessResult> userWorkflows = userWorkflowAssignments.stream()
            .map(WorkflowAssignment::getProcessResult)
            .distinct()
            .collect(Collectors.toList());
        
        List<Map<String, Object>> workflowsWithTasks = new ArrayList<>();
        for (ProcessResult workflow : userWorkflows) {
            Map<String, Object> workflowMap = new HashMap<>();
            workflowMap.put("id", workflow.getId());
            workflowMap.put("processName", workflow.getProcessName());
            workflowMap.put("status", workflow.getStatus());
            // Get tasks assigned to this user in this workflow
            List<TaskAssignment> allTasks = taskAssignmentService.getTaskAssignmentsByProcessResult(workflow);
            List<TaskAssignment> userTasks = allTasks.stream()
                .filter(t -> t.getAssignedTo().getId().equals(user.getId()))
                .collect(Collectors.toList());
            workflowMap.put("taskAssignments", userTasks);
            workflowsWithTasks.add(workflowMap);
        }
        model.addAttribute("workflowsWithTasks", workflowsWithTasks);
        
        // Count unfinished and completed tasks for user
        List<TaskAssignment> allUserTasks = taskAssignmentService.getTaskAssignmentsByUser(user);
        long unfinishedTaskCount = allUserTasks.stream()
            .filter(t -> "PENDING".equals(t.getStatus()) || "IN_PROGRESS".equals(t.getStatus()))
            .count();
        long completedTaskCount = allUserTasks.stream()
            .filter(t -> "COMPLETED".equals(t.getStatus()) || "APPROVED".equals(t.getStatus()))
            .count();
        
        model.addAttribute("user", user);
        model.addAttribute("processResults", processResults);
        model.addAttribute("unfinishedTaskCount", unfinishedTaskCount);
        model.addAttribute("completedTaskCount", completedTaskCount);
        
        return "dashboard";
    }

    @GetMapping("/process-results")
    public String processResultsList(Authentication authentication, Model model) {
        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }
        
        List<ProcessResult> processResults = processResultService.getProcessResultsByUser(user);
        model.addAttribute("processResults", processResults);
        return "process-results";
    }

    @GetMapping("/process-results/new")
    public String newProcessResultForm(Model model) {
        model.addAttribute("processResult", new ProcessResult());
        return "process-result-form";
    }

    @PostMapping("/process-results")
    public String createProcessResult(@ModelAttribute ProcessResult processResult,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }
        
        processResult.setUser(user);
        processResultService.createProcessResult(processResult);
        redirectAttributes.addFlashAttribute("message", "Process result created successfully!");
        return "redirect:/process-results";
    }

    @GetMapping("/process-results/{id}/edit")
    public String editProcessResultForm(@PathVariable Long id, Authentication authentication, Model model) {
        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }
        
        ProcessResult processResult = processResultService.getProcessResultById(id)
            .orElseThrow(() -> new RuntimeException("Process result not found"));
        
        if (!processResult.getUser().getId().equals(user.getId())) {
            return "redirect:/process-results";
        }
        
        model.addAttribute("processResult", processResult);
        return "process-result-form";
    }

    @PostMapping("/process-results/{id}")
    public String updateProcessResult(@PathVariable Long id,
                                     @ModelAttribute ProcessResult processResultDetails,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }
        
        ProcessResult existing = processResultService.getProcessResultById(id)
            .orElseThrow(() -> new RuntimeException("Process result not found"));
        
        if (!existing.getUser().getId().equals(user.getId())) {
            return "redirect:/process-results";
        }
        
        processResultService.updateProcessResult(id, processResultDetails);
        redirectAttributes.addFlashAttribute("message", "Process result updated successfully!");
        return "redirect:/process-results";
    }

    @GetMapping("/process-results/{id}/delete")
    public String deleteProcessResult(@PathVariable Long id,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }
        
        ProcessResult existing = processResultService.getProcessResultById(id)
            .orElseThrow(() -> new RuntimeException("Process result not found"));
        
        if (!existing.getUser().getId().equals(user.getId())) {
            return "redirect:/process-results";
        }
        
        processResultService.deleteProcessResult(id);
        redirectAttributes.addFlashAttribute("message", "Process result deleted successfully!");
        return "redirect:/process-results";
    }

    @PostMapping("/workflow/start")
    public String startWorkflow(@RequestParam String processKey,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }
        
        // Only ADMIN can start workflow
        if (!user.getRole().equals("ADMIN")) {
            redirectAttributes.addFlashAttribute("error", "Only administrators can start workflows!");
            return "redirect:/dashboard";
        }
        
        try {
            String processInstanceId;
            if (workflowService != null) {
                processInstanceId = workflowService.startProcess(processKey, null, user);
            } else if (workflowServiceSimple != null) {
                processInstanceId = workflowServiceSimple.startProcess(processKey, null, user);
            } else {
                redirectAttributes.addFlashAttribute("error", "Workflow service is not available");
                return "redirect:/dashboard";
            }
            redirectAttributes.addFlashAttribute("message", "Workflow started successfully! Process ID: " + processInstanceId);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error starting workflow: " + e.getMessage());
        }
        
        return "redirect:/dashboard";
    }

    @PostMapping("/workflow/tasks/{taskId}/complete")
    public String completeTask(@PathVariable String taskId,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }
        
        try {
            if (workflowService != null) {
                workflowService.completeTask(taskId, null);
            } else if (workflowServiceSimple != null) {
                workflowServiceSimple.completeTask(taskId, null);
            } else {
                redirectAttributes.addFlashAttribute("error", "Workflow service is not available");
                return "redirect:/dashboard";
            }
            redirectAttributes.addFlashAttribute("message", "Task completed successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error completing task: " + e.getMessage());
        }
        
        return "redirect:/dashboard";
    }

    @GetMapping("/workflow/create")
    public String createWorkflowForm(Authentication authentication, Model model) {
        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null || !user.getRole().equals("ADMIN")) {
            return "redirect:/dashboard";
        }
        
        List<User> users = userService.getAllUsers().stream()
            .filter(u -> u.getRole().equals("USER"))
            .collect(Collectors.toList());
        model.addAttribute("users", users);
        
        // Add users as JSON for JavaScript
        List<Map<String, Object>> usersJson = users.stream()
            .map(u -> {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", u.getId());
                userMap.put("username", u.getUsername());
                userMap.put("email", u.getEmail());
                return userMap;
            })
            .collect(Collectors.toList());
        model.addAttribute("usersJson", usersJson);
        
        // Get available BPMN process definitions
        List<Map<String, String>> processDefinitions = new ArrayList<>();
        if (workflowService != null && workflowService.isFlowableAvailable()) {
            try {
                List<org.flowable.engine.repository.ProcessDefinition> definitions = workflowService.getAvailableProcessDefinitions();
                System.out.println("=== BPMN Process Definitions Check ===");
                System.out.println("Found " + definitions.size() + " BPMN process definitions in database");
                for (org.flowable.engine.repository.ProcessDefinition def : definitions) {
                    Map<String, String> defMap = new HashMap<>();
                    defMap.put("key", def.getKey());
                    defMap.put("name", def.getName() != null ? def.getName() : def.getKey());
                    processDefinitions.add(defMap);
                    System.out.println("  - Key: " + def.getKey() + ", Name: " + def.getName() + ", Version: " + def.getVersion());
                }
                if (definitions.isEmpty()) {
                    System.out.println("WARNING: No BPMN processes found in database!");
                    System.out.println("This means BPMN files are not being auto-deployed.");
                    System.out.println("Check application logs for Flowable initialization errors.");
                }
            } catch (Exception e) {
                System.err.println("Error loading BPMN process definitions: " + e.getMessage());
                e.printStackTrace();
                // BPMN engine not available, use empty list
            }
        } else {
            System.out.println("=== Flowable Engine Status ===");
            System.out.println("WorkflowService available: " + (workflowService != null));
            if (workflowService != null) {
                System.out.println("Flowable engine available: " + workflowService.isFlowableAvailable());
            } else {
                System.out.println("WorkflowService is NULL - check if Flowable dependency is loaded");
            }
        }
        model.addAttribute("processDefinitions", processDefinitions);
        
        return "workflow-create";
    }

    @PostMapping("/workflow/create")
    public String createWorkflow(@RequestParam String workflowName,
                                 @RequestParam String workflowMode,
                                 @RequestParam(required = false) String bpmnProcessKey,
                                 @RequestParam(value = "workflowUserIds[]") Long[] workflowUserIds,
                                 @RequestParam(value = "taskNames[]", required = false) String[] taskNames,
                                 @RequestParam(value = "taskDescriptions[]", required = false) String[] taskDescriptions,
                                 @RequestParam(value = "assignedUserIds[]", required = false) Long[] assignedUserIds,
                                 @RequestParam(value = "assignedUserIds2[]", required = false) Long[] assignedUserIds2,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        User admin = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (admin == null || !admin.getRole().equals("ADMIN")) {
            redirectAttributes.addFlashAttribute("error", "Only administrators can create workflows!");
            return "redirect:/dashboard";
        }
        
        try {
            String processInstanceId;
            ProcessResult processResult = new ProcessResult();
            
            if ("BPMN".equals(workflowMode) && bpmnProcessKey != null && !bpmnProcessKey.isEmpty()) {
                // BPMN Mode: Use Flowable engine
                if (workflowService == null) {
                    redirectAttributes.addFlashAttribute("error", "BPMN engine is not available. Please use Manual mode.");
                    return "redirect:/workflow/create";
                }
                
                // Start BPMN process
                Map<String, Object> variables = new HashMap<>();
                variables.put("userId", admin.getId().toString());
                variables.put("username", admin.getUsername());
                
                // Set assignees from workflow users - map to variables used in BPMN
                // Common variable names: reviewer, approver, assignee1, assignee2, assignee3, manager, etc.
                if (workflowUserIds.length > 0) {
                    variables.put("reviewer", workflowUserIds[0].toString());
                    variables.put("reviewer1", workflowUserIds[0].toString());
                    variables.put("assignee1", workflowUserIds[0].toString());
                    if (workflowUserIds.length > 1) {
                        variables.put("approver", workflowUserIds[1].toString());
                        variables.put("assignee2", workflowUserIds[1].toString());
                        variables.put("manager", workflowUserIds[1].toString());
                    }
                    if (workflowUserIds.length > 2) {
                        variables.put("assignee3", workflowUserIds[2].toString());
                    }
                }
                
                processInstanceId = workflowService.startProcess(bpmnProcessKey, variables, admin);
                
                // Get process result created by WorkflowService
                List<ProcessResult> results = processResultService.getAllProcessResults();
                processResult = results.stream()
                    .filter(r -> r.getProcessInstanceId().equals(processInstanceId))
                    .findFirst()
                    .orElse(null);
                
                if (processResult == null) {
                    processResult = new ProcessResult();
                    processResult.setProcessName(workflowName);
                    processResult.setProcessInstanceId(processInstanceId);
                    processResult.setStatus("PENDING");
                    processResult.setResultData("BPMN Process started: " + bpmnProcessKey);
                    processResult.setUser(admin);
                    processResult = processResultService.createProcessResult(processResult);
                } else {
                    processResult.setProcessName(workflowName);
                    processResult = processResultService.updateProcessResult(processResult.getId(), processResult);
                }
                
                // Create Task Assignments from Flowable tasks for BPMN mode
                if (workflowService != null && workflowService.isFlowableAvailable()) {
                    try {
                        org.flowable.engine.TaskService taskService = workflowService.getTaskService();
                        org.flowable.engine.RepositoryService repositoryService = workflowService.getRepositoryService();
                        
                        if (taskService != null && repositoryService != null) {
                            // Get process definition to understand task structure
                            org.flowable.engine.repository.ProcessDefinition processDef = repositoryService
                                .createProcessDefinitionQuery()
                                .processDefinitionKey(bpmnProcessKey)
                                .latestVersion()
                                .singleResult();
                            
                            System.out.println("=== Creating TaskAssignments for BPMN Process: " + bpmnProcessKey + " ===");
                            System.out.println("Process Instance ID: " + processInstanceId);
                            
                            // Wait longer for tasks to be created (Flowable might need more time)
                            Thread.sleep(500);
                            
                            // Query tasks by process instance ID (without assignee filter to get all tasks)
                            List<org.flowable.task.api.Task> flowableTasks = taskService.createTaskQuery()
                                .processInstanceId(processInstanceId)
                                .list();
                            
                            System.out.println("Found " + flowableTasks.size() + " Flowable tasks for process instance: " + processInstanceId);
                            
                            // If no tasks found immediately, try again after a longer wait
                            if (flowableTasks.isEmpty()) {
                                System.out.println("No tasks found immediately, waiting longer...");
                                Thread.sleep(1000);
                                flowableTasks = taskService.createTaskQuery()
                                    .processInstanceId(processInstanceId)
                                    .list();
                                System.out.println("After longer wait, found " + flowableTasks.size() + " tasks");
                            }
                            
                            // Create a map of user IDs to User objects for quick lookup
                            Map<Long, User> userMap = new HashMap<>();
                            for (Long userId : workflowUserIds) {
                                if (userId != null) {
                                    userRepository.findById(userId).ifPresent(user -> userMap.put(userId, user));
                                }
                            }
                            
                            int taskAssignmentCount = 0;
                            for (org.flowable.task.api.Task flowableTask : flowableTasks) {
                                // Find user by assignee (could be username or user ID)
                                String assignee = flowableTask.getAssignee();
                                User assignedUser = null;
                                
                                System.out.println("Processing Flowable task: " + flowableTask.getName() + 
                                    " (ID: " + flowableTask.getId() + "), assignee: " + assignee);
                                
                                if (assignee != null && !assignee.isEmpty()) {
                                    // Try to find by ID first (if assignee is user ID from variable)
                                    try {
                                        Long userId = Long.parseLong(assignee);
                                        assignedUser = userMap.get(userId);
                                        if (assignedUser == null) {
                                            assignedUser = userRepository.findById(userId).orElse(null);
                                        }
                                        if (assignedUser != null) {
                                            System.out.println("  -> Found user by ID: " + userId + " -> " + assignedUser.getUsername());
                                        }
                                    } catch (NumberFormatException e) {
                                        // If not a number, try to find by username
                                        assignedUser = userRepository.findByUsername(assignee).orElse(null);
                                        if (assignedUser != null) {
                                            System.out.println("  -> Found user by username: " + assignee);
                                        }
                                    }
                                }
                                
                                // If assignee is not found or empty, assign to first workflow user
                                if (assignedUser == null) {
                                    if (workflowUserIds.length > 0) {
                                        assignedUser = userMap.get(workflowUserIds[0]);
                                        if (assignedUser == null) {
                                            assignedUser = userRepository.findById(workflowUserIds[0])
                                                .orElseThrow(() -> new RuntimeException("User not found: " + workflowUserIds[0]));
                                        }
                                        System.out.println("  -> Assigned to first workflow user: " + assignedUser.getUsername());
                                    } else {
                                        System.out.println("  -> WARNING: No workflow users assigned, skipping task: " + flowableTask.getName());
                                        continue;
                                    }
                                }
                                
                                if (assignedUser != null) {
                                    // Check if TaskAssignment already exists for this Flowable task
                                    List<TaskAssignment> existingAssignments = taskAssignmentService.getTaskAssignmentsByProcessResult(processResult);
                                    boolean alreadyExists = existingAssignments.stream()
                                        .anyMatch(ta -> flowableTask.getId().equals(ta.getFlowableTaskId()));
                                    
                                    if (!alreadyExists) {
                                        TaskAssignment taskAssignment = new TaskAssignment();
                                        taskAssignment.setTaskName(flowableTask.getName() != null ? flowableTask.getName() : flowableTask.getTaskDefinitionKey());
                                        taskAssignment.setDescription(flowableTask.getDescription() != null ? flowableTask.getDescription() : "");
                                        taskAssignment.setProcessResult(processResult);
                                        taskAssignment.setAssignedTo(assignedUser);
                                        taskAssignment.setAssignedBy(admin);
                                        taskAssignment.setFlowableTaskId(flowableTask.getId());
                                        taskAssignment.setStatus("PENDING");
                                        taskAssignmentService.createTaskAssignment(taskAssignment);
                                        taskAssignmentCount++;
                                        System.out.println("  -> ✓ Created TaskAssignment for task: " + taskAssignment.getTaskName() + 
                                            ", assigned to: " + assignedUser.getUsername() + " (Flowable Task ID: " + flowableTask.getId() + ")");
                                    } else {
                                        System.out.println("  -> TaskAssignment already exists for Flowable task: " + flowableTask.getId());
                                    }
                                }
                            }
                            
                            if (flowableTasks.isEmpty()) {
                                System.out.println("⚠ WARNING: No Flowable tasks found for process instance: " + processInstanceId);
                                System.out.println("This might mean:");
                                System.out.println("  1. The BPMN process has no user tasks");
                                System.out.println("  2. Tasks are created asynchronously (check later)");
                                System.out.println("  3. The process definition might have issues");
                            } else {
                                System.out.println("✓ Successfully created " + taskAssignmentCount + " TaskAssignment(s) from " + flowableTasks.size() + " Flowable task(s)");
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("❌ Error creating TaskAssignments from Flowable tasks: " + e.getMessage());
                        e.printStackTrace();
                        // Continue even if task assignment creation fails
                    }
                }
                
            } else {
                // Manual Mode: Create workflow manually
                if (taskNames == null || taskNames.length == 0) {
                    redirectAttributes.addFlashAttribute("error", "Please add at least one task for manual workflow!");
                    return "redirect:/workflow/create";
                }
                
                // Generate process instance ID manually
                processInstanceId = "workflow_" + System.currentTimeMillis() + "_" + admin.getId();
                
                // Create ProcessResult
                processResult.setProcessName(workflowName);
                processResult.setProcessInstanceId(processInstanceId);
                processResult.setStatus("PENDING");
                processResult.setResultData("Manual workflow created with " + taskNames.length + " task(s)");
                processResult.setUser(admin);
                processResult = processResultService.createProcessResult(processResult);
            }
            
            // Create Workflow Assignments (assign users to workflow) - for both modes
            System.out.println("=== Creating Workflow Assignments ===");
            System.out.println("Process Result ID: " + processResult.getId());
            System.out.println("Workflow Name: " + processResult.getProcessName());
            System.out.println("Number of users to assign: " + workflowUserIds.length);
            
            java.util.Set<Long> workflowUserSet = new java.util.HashSet<>();
            int workflowAssignmentCount = 0;
            for (Long userId : workflowUserIds) {
                if (userId != null) {
                    workflowUserSet.add(userId);
                    User assignedUser = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("User not found: " + userId));
                    
                    WorkflowAssignment workflowAssignment = new WorkflowAssignment();
                    workflowAssignment.setProcessResult(processResult);
                    workflowAssignment.setAssignedTo(assignedUser);
                    workflowAssignment.setAssignedBy(admin);
                    workflowAssignmentService.createWorkflowAssignment(workflowAssignment);
                    workflowAssignmentCount++;
                    System.out.println("  ✓ Created WorkflowAssignment for user: " + assignedUser.getUsername() + " (ID: " + userId + ")");
                }
            }
            System.out.println("✓ Successfully created " + workflowAssignmentCount + " WorkflowAssignment(s)");
            
            // Create Task Assignments - hanya untuk Manual Mode
            int taskCount = 0;
            if ("MANUAL".equals(workflowMode) && taskNames != null) {
                for (int i = 0; i < taskNames.length; i++) {
                    final int taskIndex = i;
                    String taskName = taskNames[taskIndex];
                    String taskDescription = taskDescriptions != null && taskDescriptions.length > taskIndex ? taskDescriptions[taskIndex] : "";
                    
                    // User 1 (required)
                    if (assignedUserIds != null && taskIndex < assignedUserIds.length && assignedUserIds[taskIndex] != null) {
                        Long userId1 = assignedUserIds[taskIndex];
                        
                        // Validasi: user harus sudah di-assign di workflow
                        if (!workflowUserSet.contains(userId1)) {
                            throw new RuntimeException("User " + userId1 + " is not assigned to this workflow. Please assign user to workflow first.");
                        }
                        
                        User assignedTo1 = userRepository.findById(userId1)
                            .orElseThrow(() -> new RuntimeException("User not found: " + userId1));
                        
                        TaskAssignment taskAssignment1 = new TaskAssignment();
                        taskAssignment1.setTaskName(taskName);
                        taskAssignment1.setDescription(taskDescription);
                        taskAssignment1.setProcessResult(processResult);
                        taskAssignment1.setAssignedTo(assignedTo1);
                        taskAssignment1.setAssignedBy(admin);
                        taskAssignment1.setFlowableTaskId("");
                        taskAssignmentService.createTaskAssignment(taskAssignment1);
                        taskCount++;
                    }
                    
                    // User 2 (optional)
                    if (assignedUserIds2 != null && taskIndex < assignedUserIds2.length && assignedUserIds2[taskIndex] != null && assignedUserIds2[taskIndex] > 0) {
                        Long userId2 = assignedUserIds2[taskIndex];
                        
                        // Validasi: user harus sudah di-assign di workflow
                        if (!workflowUserSet.contains(userId2)) {
                            throw new RuntimeException("User " + userId2 + " is not assigned to this workflow. Please assign user to workflow first.");
                        }
                        
                        User assignedTo2 = userRepository.findById(userId2)
                            .orElseThrow(() -> new RuntimeException("User not found: " + userId2));
                        
                        TaskAssignment taskAssignment2 = new TaskAssignment();
                        taskAssignment2.setTaskName(taskName);
                        taskAssignment2.setDescription(taskDescription);
                        taskAssignment2.setProcessResult(processResult);
                        taskAssignment2.setAssignedTo(assignedTo2);
                        taskAssignment2.setAssignedBy(admin);
                        taskAssignment2.setFlowableTaskId("");
                        taskAssignmentService.createTaskAssignment(taskAssignment2);
                        taskCount++;
                    }
                }
            }
            
            // Count task assignments for BPMN mode
            int bpmnTaskCount = 0;
            if ("BPMN".equals(workflowMode)) {
                List<TaskAssignment> bpmnTasks = taskAssignmentService.getTaskAssignmentsByProcessResult(processResult);
                bpmnTaskCount = bpmnTasks.size();
                System.out.println("=== BPMN Workflow Summary ===");
                System.out.println("Process Result ID: " + processResult.getId());
                System.out.println("Process Instance ID: " + processResult.getProcessInstanceId());
                System.out.println("Workflow Name: " + processResult.getProcessName());
                System.out.println("Workflow Assignments: " + workflowAssignmentCount);
                System.out.println("Task Assignments: " + bpmnTaskCount);
            }
            
            String successMessage;
            if ("BPMN".equals(workflowMode)) {
                successMessage = "BPMN workflow '" + workflowName + "' created successfully! " + 
                    workflowAssignmentCount + " user(s) assigned, " + bpmnTaskCount + " task(s) created. Process Instance ID: " + processInstanceId;
            } else {
                successMessage = "Manual workflow created successfully with " + workflowUserIds.length + " user(s) assigned and " + taskNames.length + " task(s) with " + taskCount + " assignment(s)!";
            }
            redirectAttributes.addFlashAttribute("message", successMessage);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating workflow: " + e.getMessage());
            e.printStackTrace();
        }
        
        return "redirect:/dashboard";
    }

    @GetMapping("/tasks/{id}/complete")
    public String completeTaskForm(@PathVariable Long id, Authentication authentication, Model model) {
        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }
        
        TaskAssignment taskAssignment = taskAssignmentService.getTaskAssignmentById(id)
            .orElseThrow(() -> new RuntimeException("Task assignment not found"));
        
        if (!taskAssignment.getAssignedTo().getId().equals(user.getId())) {
            return "redirect:/dashboard";
        }
        
        model.addAttribute("taskAssignment", taskAssignment);
        return "task-complete";
    }

    @PostMapping("/tasks/{id}/complete")
    public String completeTask(@PathVariable Long id,
                              @RequestParam String userNotes,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }
        
        try {
            taskAssignmentService.completeTask(id, userNotes, user);
            redirectAttributes.addFlashAttribute("message", "Task completed successfully! Waiting for admin approval.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error completing task: " + e.getMessage());
        }
        
        return "redirect:/dashboard";
    }

    @GetMapping("/admin/tasks/review")
    public String reviewTasks(Authentication authentication, Model model) {
        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null || !user.getRole().equals("ADMIN")) {
            return "redirect:/dashboard";
        }
        
        List<TaskAssignment> allTasks = taskAssignmentService.getAllTaskAssignments();
        List<TaskAssignment> completedTasks = allTasks.stream()
            .filter(t -> "COMPLETED".equals(t.getStatus()))
            .collect(Collectors.toList());
        
        model.addAttribute("completedTasks", completedTasks);
        return "admin-task-review";
    }

    @PostMapping("/admin/tasks/{id}/approve")
    public String approveTask(@PathVariable Long id,
                             @RequestParam(required = false) String adminNotes,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        User admin = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (admin == null || !admin.getRole().equals("ADMIN")) {
            return "redirect:/dashboard";
        }
        
        try {
            TaskAssignment approvedTask = taskAssignmentService.approveTask(id, adminNotes, admin);
            redirectAttributes.addFlashAttribute("message", "Task approved successfully!");
            
            // Set notification flag (will be checked by user via polling)
            approvedTask.setStatus("APPROVED");
            taskAssignmentService.updateTaskAssignment(approvedTask);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error approving task: " + e.getMessage());
        }
        
        return "redirect:/admin/tasks/review";
    }
    
    @GetMapping("/api/check-approvals")
    @ResponseBody
    public Map<String, Object> checkRecentApprovals(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        
        if (user == null) {
            response.put("hasNewApprovals", false);
            return response;
        }
        
        // Get recently approved tasks (dalam 10 detik terakhir)
        List<TaskAssignment> userTasks = taskAssignmentService.getTaskAssignmentsByUser(user);
        List<TaskAssignment> recentlyApproved = userTasks.stream()
            .filter(t -> "APPROVED".equals(t.getStatus()) && t.getApprovedAt() != null)
            .filter(t -> t.getApprovedAt().isAfter(java.time.LocalDateTime.now().minusSeconds(10)))
            .collect(Collectors.toList());
        
        if (!recentlyApproved.isEmpty()) {
            response.put("hasNewApprovals", true);
            List<Map<String, String>> approvals = new ArrayList<>();
            for (TaskAssignment task : recentlyApproved) {
                Map<String, String> approval = new HashMap<>();
                approval.put("taskName", task.getTaskName());
                approval.put("workflowName", task.getProcessResult().getProcessName());
                approvals.add(approval);
            }
            response.put("approvals", approvals);
        } else {
            response.put("hasNewApprovals", false);
        }
        
        return response;
    }

    @GetMapping("/completed-workflows")
    public String completedWorkflows(Authentication authentication, Model model) {
        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }
        
        // Get completed workflows for this user
        List<ProcessResult> allResults = processResultService.getAllProcessResults();
        List<ProcessResult> completedWorkflows = allResults.stream()
            .filter(pr -> "COMPLETED".equals(pr.getStatus()))
            .filter(pr -> {
                // Check if user has any task in this workflow
                List<TaskAssignment> tasks = taskAssignmentService.getTaskAssignmentsByProcessResult(pr);
                return tasks.stream().anyMatch(t -> t.getAssignedTo().getId().equals(user.getId()));
            })
            .collect(Collectors.toList());
        
        // Add task assignments for each workflow
        List<Map<String, Object>> workflowsWithTasks = new ArrayList<>();
        for (ProcessResult workflow : completedWorkflows) {
            Map<String, Object> workflowMap = new HashMap<>();
            workflowMap.put("id", workflow.getId());
            workflowMap.put("processName", workflow.getProcessName());
            workflowMap.put("status", workflow.getStatus());
            workflowMap.put("createdAt", workflow.getCreatedAt());
            workflowMap.put("completedAt", workflow.getCompletedAt());
            workflowMap.put("resultData", workflow.getResultData());
            
            // Get tasks assigned to this user
            List<TaskAssignment> allTasks = taskAssignmentService.getTaskAssignmentsByProcessResult(workflow);
            List<TaskAssignment> userTasks = allTasks.stream()
                .filter(t -> t.getAssignedTo().getId().equals(user.getId()))
                .collect(Collectors.toList());
            workflowMap.put("taskAssignments", userTasks);
            
            workflowsWithTasks.add(workflowMap);
        }
        
        model.addAttribute("workflowsWithTasks", workflowsWithTasks);
        model.addAttribute("user", user);
        
        return "completed-workflows";
    }
}

