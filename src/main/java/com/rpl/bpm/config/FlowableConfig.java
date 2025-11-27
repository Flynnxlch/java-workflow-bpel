package com.rpl.bpm.config;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FlowableConfig implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(FlowableConfig.class);

    private final ApplicationContext applicationContext;

    public FlowableConfig(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("=== Flowable Engine Initialization Check ===");
        
        // Try to get ProcessEngine from context
        org.flowable.engine.ProcessEngine processEngine = null;
        RepositoryService repositoryService = null;
        
        try {
            processEngine = applicationContext.getBean(org.flowable.engine.ProcessEngine.class);
            repositoryService = applicationContext.getBean(RepositoryService.class);
        } catch (Exception e) {
            logger.warn("Flowable beans not found in context: {}", e.getMessage());
        }
        
        logger.info("ProcessEngine available: {}", processEngine != null);
        logger.info("RepositoryService available: {}", repositoryService != null);
        
        if (processEngine == null) {
            logger.error("=== Flowable ProcessEngine is NULL - BPMN engine not initialized! ===");
            logger.error("Please check:");
            logger.error("1. Flowable dependency is in pom.xml (flowable-spring-boot-starter)");
            logger.error("2. Database connection is working");
            logger.error("3. Flowable auto-configuration is enabled");
            logger.error("4. Check application.properties for Flowable configuration");
            logger.error("5. Check if there are any errors during Flowable initialization");
            return;
        }
        
        if (repositoryService == null) {
            logger.error("=== Flowable RepositoryService is NULL - BPMN engine not initialized! ===");
            logger.error("ProcessEngine exists but RepositoryService is null - this is unusual!");
            return;
        }
        
        try {
            logger.info("=== Checking Flowable BPMN Engine Status ===");
            
            // Check if BPMN files need to be deployed
            long existingCount = repositoryService.createProcessDefinitionQuery().count();
            logger.info("Existing process definitions in database: {}", existingCount);
            
            if (existingCount == 0) {
                logger.info("No process definitions found. Attempting to deploy BPMN files...");
                
                // Try to deploy BPMN files explicitly - deploy one by one to handle errors
                try {
                    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
                    Resource[] resources = resolver.getResources("classpath:/processes/*.bpmn20.xml");
                    
                    if (resources != null && resources.length > 0) {
                        logger.info("Found {} BPMN files to deploy", resources.length);
                        
                        int successCount = 0;
                        int failCount = 0;
                        
                        // Deploy each file individually to handle errors gracefully
                        for (Resource resource : resources) {
                            try {
                                org.flowable.engine.repository.DeploymentBuilder deploymentBuilder = 
                                    repositoryService.createDeployment()
                                        .name("BPMN Process: " + resource.getFilename())
                                        .addInputStream(resource.getFilename(), resource.getInputStream());
                                
                                org.flowable.engine.repository.Deployment deployment = deploymentBuilder.deploy();
                                logger.info("  ✓ Successfully deployed: {} (Deployment ID: {})", 
                                    resource.getFilename(), deployment.getId());
                                successCount++;
                            } catch (Exception fileEx) {
                                logger.warn("  ✗ Failed to deploy {}: {}", 
                                    resource.getFilename(), fileEx.getMessage());
                                failCount++;
                                // Continue with next file
                            }
                        }
                        
                        logger.info("Deployment summary: {} succeeded, {} failed", successCount, failCount);
                    } else {
                        logger.warn("No BPMN files found in classpath:/processes/*.bpmn20.xml");
                    }
                } catch (Exception deployEx) {
                    logger.error("Error loading BPMN files: {}", deployEx.getMessage(), deployEx);
                }
            }
            
            // Check again after deployment
            List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
                .latestVersion()
                .list();
            
            logger.info("=== Flowable BPMN Process Definitions ===");
            logger.info("Found {} BPMN process definition(s)", processDefinitions.size());
            
            if (processDefinitions.isEmpty()) {
                logger.warn("No BPMN process definitions found in database!");
                logger.warn("This could mean:");
                logger.warn("1. BPMN files are not being auto-deployed");
                logger.warn("2. BPMN files are in wrong location (should be: src/main/resources/processes/)");
                logger.warn("3. Flowable auto-deployment is disabled");
            } else {
                for (ProcessDefinition def : processDefinitions) {
                    logger.info("  - Process Key: {}, Name: {}, Version: {}", 
                        def.getKey(), def.getName(), def.getVersion());
                }
            }
        } catch (Exception e) {
            logger.error("Error loading BPMN process definitions: {}", e.getMessage(), e);
            e.printStackTrace();
        }
    }
}

