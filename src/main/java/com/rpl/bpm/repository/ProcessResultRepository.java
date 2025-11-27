package com.rpl.bpm.repository;

import com.rpl.bpm.entity.ProcessResult;
import com.rpl.bpm.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcessResultRepository extends JpaRepository<ProcessResult, Long> {
    List<ProcessResult> findByUser(User user);
    List<ProcessResult> findByStatus(String status);
    List<ProcessResult> findByProcessName(String processName);
}

