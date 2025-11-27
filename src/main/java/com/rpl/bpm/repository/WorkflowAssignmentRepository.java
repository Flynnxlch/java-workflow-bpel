package com.rpl.bpm.repository;

import com.rpl.bpm.entity.ProcessResult;
import com.rpl.bpm.entity.User;
import com.rpl.bpm.entity.WorkflowAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowAssignmentRepository extends JpaRepository<WorkflowAssignment, Long> {
    List<WorkflowAssignment> findByProcessResult(ProcessResult processResult);
    List<WorkflowAssignment> findByAssignedTo(User user);
    boolean existsByProcessResultAndAssignedTo(ProcessResult processResult, User user);
}

