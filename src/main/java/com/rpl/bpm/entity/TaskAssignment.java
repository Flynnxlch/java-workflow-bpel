package com.rpl.bpm.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "task_assignments")
public class TaskAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String taskName;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private String status; // PENDING, IN_PROGRESS, COMPLETED, APPROVED
    
    @Column(columnDefinition = "TEXT")
    private String userNotes; // Keterangan dari user saat complete
    
    @Column(columnDefinition = "TEXT")
    private String adminNotes; // Keterangan dari admin saat approve/reject
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime completedAt;
    
    private LocalDateTime approvedAt;
    
    @ManyToOne
    @JoinColumn(name = "process_result_id", nullable = false)
    private ProcessResult processResult;
    
    @ManyToOne
    @JoinColumn(name = "assigned_to_user_id", nullable = false)
    private User assignedTo; // User yang ditugaskan
    
    @ManyToOne
    @JoinColumn(name = "assigned_by_user_id", nullable = false)
    private User assignedBy; // Admin yang memberikan tugas
    
    @Column(nullable = false)
    private String flowableTaskId; // ID task dari Flowable (jika ada)

    public TaskAssignment() {
    }

    public TaskAssignment(Long id, String taskName, String description, String status, String userNotes, String adminNotes, LocalDateTime createdAt, LocalDateTime completedAt, LocalDateTime approvedAt, ProcessResult processResult, User assignedTo, User assignedBy, String flowableTaskId) {
        this.id = id;
        this.taskName = taskName;
        this.description = description;
        this.status = status;
        this.userNotes = userNotes;
        this.adminNotes = adminNotes;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        this.approvedAt = approvedAt;
        this.processResult = processResult;
        this.assignedTo = assignedTo;
        this.assignedBy = assignedBy;
        this.flowableTaskId = flowableTaskId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUserNotes() {
        return userNotes;
    }

    public void setUserNotes(String userNotes) {
        this.userNotes = userNotes;
    }

    public String getAdminNotes() {
        return adminNotes;
    }

    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
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

    public String getFlowableTaskId() {
        return flowableTaskId;
    }

    public void setFlowableTaskId(String flowableTaskId) {
        this.flowableTaskId = flowableTaskId;
    }
}
