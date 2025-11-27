package com.rpl.bpm.service;

import com.rpl.bpm.entity.ProcessResult;
import com.rpl.bpm.entity.TaskAssignment;
import com.rpl.bpm.entity.User;
import com.rpl.bpm.repository.TaskAssignmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TaskAssignmentService {

    @Autowired
    private TaskAssignmentRepository taskAssignmentRepository;

    public TaskAssignment createTaskAssignment(TaskAssignment taskAssignment) {
        taskAssignment.setStatus("PENDING");
        taskAssignment.setCreatedAt(LocalDateTime.now());
        return taskAssignmentRepository.save(taskAssignment);
    }

    public List<TaskAssignment> getTaskAssignmentsByProcessResult(ProcessResult processResult) {
        return taskAssignmentRepository.findByProcessResult(processResult);
    }

    public List<TaskAssignment> getTaskAssignmentsByUser(User user) {
        return taskAssignmentRepository.findByAssignedTo(user);
    }

    public List<TaskAssignment> getAllTaskAssignments() {
        return taskAssignmentRepository.findAll();
    }

    public List<TaskAssignment> getPendingTaskAssignmentsByUser(User user) {
        return taskAssignmentRepository.findByAssignedToAndStatus(user, "PENDING");
    }

    public Optional<TaskAssignment> getTaskAssignmentById(Long id) {
        return taskAssignmentRepository.findById(id);
    }

    public TaskAssignment updateTaskAssignment(TaskAssignment taskAssignment) {
        return taskAssignmentRepository.save(taskAssignment);
    }

    public TaskAssignment completeTask(Long taskId, String userNotes, User user) {
        TaskAssignment task = taskAssignmentRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("Task assignment not found"));
        
        if (!task.getAssignedTo().getId().equals(user.getId())) {
            throw new RuntimeException("You are not assigned to this task");
        }
        
        if (!"PENDING".equals(task.getStatus()) && !"IN_PROGRESS".equals(task.getStatus())) {
            throw new RuntimeException("Task is not in a completable state");
        }
        
        task.setStatus("COMPLETED");
        task.setUserNotes(userNotes);
        task.setCompletedAt(LocalDateTime.now());
        
        return taskAssignmentRepository.save(task);
    }

    public TaskAssignment approveTask(Long taskId, String adminNotes, User admin) {
        TaskAssignment task = taskAssignmentRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("Task assignment not found"));
        
        if (!"COMPLETED".equals(task.getStatus())) {
            throw new RuntimeException("Task must be completed before approval");
        }
        
        task.setStatus("APPROVED");
        task.setAdminNotes(adminNotes);
        task.setApprovedAt(LocalDateTime.now());
        
        // Update ProcessResult status jika semua task sudah approved
        ProcessResult processResult = task.getProcessResult();
        List<TaskAssignment> allTasks = taskAssignmentRepository.findByProcessResult(processResult);
        boolean allApproved = allTasks.stream()
            .allMatch(t -> "APPROVED".equals(t.getStatus()) || "FAILED".equals(t.getStatus()));
        
        if (allApproved) {
            processResult.setStatus("COMPLETED");
            processResult.setCompletedAt(LocalDateTime.now());
        }
        
        return taskAssignmentRepository.save(task);
    }

    public void deleteTaskAssignment(Long id) {
        taskAssignmentRepository.deleteById(id);
    }
}

