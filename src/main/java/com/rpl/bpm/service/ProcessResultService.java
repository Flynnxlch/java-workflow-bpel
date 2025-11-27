package com.rpl.bpm.service;

import com.rpl.bpm.entity.ProcessResult;
import com.rpl.bpm.entity.TaskAssignment;
import com.rpl.bpm.entity.User;
import com.rpl.bpm.entity.WorkflowAssignment;
import com.rpl.bpm.repository.ProcessResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProcessResultService {

    @Autowired
    private ProcessResultRepository processResultRepository;
    
    @Autowired
    private WorkflowAssignmentService workflowAssignmentService;
    
    @Autowired
    private TaskAssignmentService taskAssignmentService;

    public ProcessResult createProcessResult(ProcessResult processResult) {
        processResult.setCreatedAt(LocalDateTime.now());
        return processResultRepository.save(processResult);
    }

    public List<ProcessResult> getAllProcessResults() {
        return processResultRepository.findAll();
    }

    public List<ProcessResult> getProcessResultsByUser(User user) {
        return processResultRepository.findByUser(user);
    }

    public Optional<ProcessResult> getProcessResultById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return processResultRepository.findById(id);
    }

    public ProcessResult updateProcessResult(Long id, ProcessResult processResultDetails) {
        if (id == null) {
            throw new RuntimeException("Process result ID cannot be null");
        }
        ProcessResult processResult = processResultRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Process result not found"));
        
        processResult.setProcessName(processResultDetails.getProcessName());
        // Result Data tidak dapat diubah (read-only)
        // processResult.setResultData(processResultDetails.getResultData());
        processResult.setStatus(processResultDetails.getStatus());
        
        if ("COMPLETED".equals(processResultDetails.getStatus()) || 
            "FAILED".equals(processResultDetails.getStatus())) {
            processResult.setCompletedAt(LocalDateTime.now());
        }
        
        return processResultRepository.save(processResult);
    }

    public void deleteProcessResult(Long id) {
        if (id == null) {
            throw new RuntimeException("Process result ID cannot be null");
        }
        
        ProcessResult processResult = processResultRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Process result not found"));
        
        // Hapus semua TaskAssignment yang terkait
        List<TaskAssignment> taskAssignments = taskAssignmentService.getTaskAssignmentsByProcessResult(processResult);
        for (TaskAssignment task : taskAssignments) {
            taskAssignmentService.deleteTaskAssignment(task.getId());
        }
        
        // Hapus semua WorkflowAssignment yang terkait
        List<WorkflowAssignment> workflowAssignments = workflowAssignmentService.getWorkflowAssignmentsByProcessResult(processResult);
        for (WorkflowAssignment assignment : workflowAssignments) {
            workflowAssignmentService.deleteWorkflowAssignment(assignment.getId());
        }
        
        // Baru hapus ProcessResult
        processResultRepository.deleteById(id);
    }

    public List<ProcessResult> getProcessResultsByStatus(String status) {
        return processResultRepository.findByStatus(status);
    }
}

