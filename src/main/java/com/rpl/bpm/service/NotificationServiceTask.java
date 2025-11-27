package com.rpl.bpm.service;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
public class NotificationServiceTask implements JavaDelegate {
    
    @Override
    public void execute(DelegateExecution execution) {
        String username = (String) execution.getVariable("username");
        Boolean approved = (Boolean) execution.getVariable("approved");
        
        String message;
        if (approved != null && approved) {
            message = "Document approved by " + username;
        } else {
            message = "Document rejected by " + username;
        }
        
        System.out.println("Notification: " + message);
        
        // Set result variable
        execution.setVariable("notification", message);
        execution.setVariable("result", "Process completed: " + message);
    }
}

