package com.rpl.bpm.controller;

import com.rpl.bpm.entity.ProcessResult;
import com.rpl.bpm.entity.User;
import com.rpl.bpm.repository.UserRepository;
import com.rpl.bpm.service.ProcessResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/process-results")
public class ProcessResultRestController {

    @Autowired
    private ProcessResultService processResultService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<ProcessResult>> getAllProcessResults(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        List<ProcessResult> results = processResultService.getProcessResultsByUser(user);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProcessResult> getProcessResultById(@PathVariable Long id, Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Optional<ProcessResult> result = processResultService.getProcessResultById(id);
        if (result.isPresent() && result.get().getUser().getId().equals(user.getId())) {
            return ResponseEntity.ok(result.get());
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createProcessResult(
            @RequestBody ProcessResult processResult,
            Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        processResult.setUser(user);
        ProcessResult created = processResultService.createProcessResult(processResult);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Process result created successfully");
        response.put("data", created);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateProcessResult(
            @PathVariable Long id,
            @RequestBody ProcessResult processResultDetails,
            Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Optional<ProcessResult> existing = processResultService.getProcessResultById(id);
        if (existing.isEmpty() || !existing.get().getUser().getId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }
        
        ProcessResult updated = processResultService.updateProcessResult(id, processResultDetails);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Process result updated successfully");
        response.put("data", updated);
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteProcessResult(
            @PathVariable Long id,
            Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Optional<ProcessResult> existing = processResultService.getProcessResultById(id);
        if (existing.isEmpty() || !existing.get().getUser().getId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }
        
        processResultService.deleteProcessResult(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Process result deleted successfully");
        
        return ResponseEntity.ok(response);
    }
}

