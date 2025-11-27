package com.rpl.bpm.config;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.List;

@Configuration
@ConditionalOnMissingBean(ProcessEngine.class)
public class FlowableProcessEngineConfig {

    private static final Logger logger = LoggerFactory.getLogger(FlowableProcessEngineConfig.class);

    private final DataSource dataSource;

    public FlowableProcessEngineConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Bean
    @ConditionalOnMissingBean(name = "transactionManager")
    @org.springframework.context.annotation.Primary
    public PlatformTransactionManager transactionManager() {
        logger.info("Creating DataSourceTransactionManager for JPA");
        return new DataSourceTransactionManager(dataSource);
    }
    
    @Bean
    @ConditionalOnMissingBean(name = "flowableTransactionManager")
    public PlatformTransactionManager flowableTransactionManager(PlatformTransactionManager transactionManager) {
        logger.info("Using existing transactionManager for Flowable");
        return transactionManager;
    }

    @Bean
    public ProcessEngine processEngine(PlatformTransactionManager transactionManager) {
        logger.info("=== Creating Flowable ProcessEngine explicitly ===");
        
        try {
            SpringProcessEngineConfiguration configuration = new SpringProcessEngineConfiguration();
            configuration.setDataSource(dataSource);
            configuration.setTransactionManager(transactionManager);
            configuration.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
            
            // Set BPMN deployment location
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = null;
            try {
                resources = resolver.getResources("classpath:/processes/*.bpmn20.xml");
                logger.info("Found {} BPMN files in classpath", resources.length);
                for (Resource resource : resources) {
                    logger.info("  - BPMN file: {}", resource.getFilename());
                }
                configuration.setDeploymentResources(resources);
            } catch (IOException e) {
                logger.warn("Could not load BPMN files from classpath: {}", e.getMessage());
            }
            
            ProcessEngine processEngine = configuration.buildProcessEngine();
            logger.info("Flowable ProcessEngine created successfully!");
            
            // Explicitly deploy BPMN files if not already deployed
            RepositoryService repoService = processEngine.getRepositoryService();
            
            // Check if resources were found
            if (resources != null && resources.length > 0) {
                logger.info("Found {} BPMN files to deploy", resources.length);
                for (Resource resource : resources) {
                    logger.info("  - BPMN file: {}", resource.getFilename());
                }
                
                // Deploy resources explicitly
                try {
                    org.flowable.engine.repository.DeploymentBuilder deploymentBuilder = 
                        repoService.createDeployment()
                            .name("BPMN Processes Deployment");
                    
                    for (Resource resource : resources) {
                        deploymentBuilder.addInputStream(resource.getFilename(), resource.getInputStream());
                        logger.info("Adding {} to deployment", resource.getFilename());
                    }
                    
                    org.flowable.engine.repository.Deployment deployment = deploymentBuilder.deploy();
                    logger.info("Deployment successful! Deployment ID: {}", deployment.getId());
                } catch (Exception deployEx) {
                    logger.warn("Error during explicit deployment, trying to continue: {}", deployEx.getMessage());
                    // Continue even if deployment fails, might already be deployed
                }
            } else {
                logger.warn("No BPMN files found in classpath:/processes/*.bpmn20.xml");
            }
            
            // Log deployed processes
            long processCount = repoService.createProcessDefinitionQuery().count();
            logger.info("Total deployed process definition(s): {}", processCount);
            
            if (processCount > 0) {
                List<org.flowable.engine.repository.ProcessDefinition> definitions = 
                    repoService.createProcessDefinitionQuery().list();
                for (org.flowable.engine.repository.ProcessDefinition def : definitions) {
                    logger.info("  - Process: {} (Key: {}, Version: {})", 
                        def.getName(), def.getKey(), def.getVersion());
                }
            }
            
            return processEngine;
        } catch (Exception e) {
            logger.error("Error creating Flowable ProcessEngine: {}", e.getMessage(), e);
            e.printStackTrace();
            throw new RuntimeException("Failed to create Flowable ProcessEngine", e);
        }
    }
    
    @Bean
    @ConditionalOnMissingBean(RepositoryService.class)
    public RepositoryService repositoryService(ProcessEngine processEngine) {
        logger.info("Creating RepositoryService bean from ProcessEngine");
        return processEngine.getRepositoryService();
    }
    
    @Bean
    @ConditionalOnMissingBean(RuntimeService.class)
    public RuntimeService runtimeService(ProcessEngine processEngine) {
        logger.info("Creating RuntimeService bean from ProcessEngine");
        return processEngine.getRuntimeService();
    }
    
    @Bean
    @ConditionalOnMissingBean(TaskService.class)
    public TaskService taskService(ProcessEngine processEngine) {
        logger.info("Creating TaskService bean from ProcessEngine");
        return processEngine.getTaskService();
    }
}
