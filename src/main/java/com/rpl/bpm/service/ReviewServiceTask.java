package com.rpl.bpm.service;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
public class ReviewServiceTask implements JavaDelegate {
    
    @Override
    public void execute(DelegateExecution execution) {
        String username = (String) execution.getVariable("username");
        System.out.println("Processing review from user: " + username);
        
        // Simulate review processing
        execution.setVariable("reviewStatus", "Processed");
        execution.setVariable("result", "Review completed successfully by " + username);
    }
}

