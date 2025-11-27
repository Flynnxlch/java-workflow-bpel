package com.rpl.bpm.repository;

import com.rpl.bpm.entity.ProcessResult;
import com.rpl.bpm.entity.TaskAssignment;
import com.rpl.bpm.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, Long> {
    List<TaskAssignment> findByProcessResult(ProcessResult processResult);
    List<TaskAssignment> findByAssignedTo(User user);
    List<TaskAssignment> findByAssignedToAndStatus(User user, String status);
    List<TaskAssignment> findByProcessResultAndStatus(ProcessResult processResult, String status);
}

