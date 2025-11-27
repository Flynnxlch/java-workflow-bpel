package com.rpl.bpm.service;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
public class SampleServiceTask implements JavaDelegate {
    
    @Override
    public void execute(DelegateExecution execution) {
        String username = (String) execution.getVariable("username");
        System.out.println("Processing data for user: " + username);
        
        // Set result variable
        execution.setVariable("result", "Process completed successfully for " + username);
    }
}

