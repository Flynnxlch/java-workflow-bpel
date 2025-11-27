package com.rpl.bpm.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "workflow_assignments")
public class WorkflowAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "process_result_id", nullable = false)
    private ProcessResult processResult;
    
    @ManyToOne
    @JoinColumn(name = "assigned_to_user_id", nullable = false)
    private User assignedTo; // User yang di-assign ke workflow
    
    @ManyToOne
    @JoinColumn(name = "assigned_by_user_id", nullable = false)
    private User assignedBy; // Admin yang memberikan assignment
    
    @Column(nullable = false)
    private LocalDateTime assignedAt = LocalDateTime.now();

    public WorkflowAssignment() {
    }

    public WorkflowAssignment(Long id, ProcessResult processResult, User assignedTo, User assignedBy, LocalDateTime assignedAt) {
        this.id = id;
        this.processResult = processResult;
        this.assignedTo = assignedTo;
        this.assignedBy = assignedBy;
        this.assignedAt = assignedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ProcessResult getProcessResult() {
        return processResult;
    }

    public void setProcessResult(ProcessResult processResult) {
        this.processResult = processResult;
    }

    public User getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(User assignedTo) {
        this.assignedTo = assignedTo;
    }

    public User getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(User assignedBy) {
        this.assignedBy = assignedBy;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }
}
