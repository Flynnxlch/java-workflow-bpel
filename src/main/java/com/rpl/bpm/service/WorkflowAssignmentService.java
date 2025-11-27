package com.rpl.bpm.service;

import com.rpl.bpm.entity.ProcessResult;
import com.rpl.bpm.entity.User;
import com.rpl.bpm.entity.WorkflowAssignment;
import com.rpl.bpm.repository.WorkflowAssignmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class WorkflowAssignmentService {

    @Autowired
    private WorkflowAssignmentRepository workflowAssignmentRepository;

    public WorkflowAssignment createWorkflowAssignment(WorkflowAssignment workflowAssignment) {
        workflowAssignment.setAssignedAt(LocalDateTime.now());
        return workflowAssignmentRepository.save(workflowAssignment);
    }

    public List<WorkflowAssignment> getWorkflowAssignmentsByProcessResult(ProcessResult processResult) {
        return workflowAssignmentRepository.findByProcessResult(processResult);
    }

    public List<WorkflowAssignment> getWorkflowAssignmentsByUser(User user) {
        return workflowAssignmentRepository.findByAssignedTo(user);
    }

    public boolean isUserAssignedToWorkflow(ProcessResult processResult, User user) {
        return workflowAssignmentRepository.existsByProcessResultAndAssignedTo(processResult, user);
    }

    public List<User> getAssignedUsersForWorkflow(ProcessResult processResult) {
        return workflowAssignmentRepository.findByProcessResult(processResult).stream()
            .map(WorkflowAssignment::getAssignedTo)
            .distinct()
            .toList();
    }
    
    public void deleteWorkflowAssignment(Long id) {
        if (id == null) {
            throw new RuntimeException("Workflow assignment ID cannot be null");
        }
        workflowAssignmentRepository.deleteById(id);
    }
}

