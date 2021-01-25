package com.marklogic.analyzer;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.script.ScriptException;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;



@SpringBootApplication
@Configuration
@EnableAutoConfiguration
@ComponentScan
@EnableAsync
@PropertySource(value = { "classpath:user.properties" }, ignoreResourceNotFound = true)
public class AnalyzerApplication {
   @Value("${management.security.enabled}")
   private String MGMT_SECURITY;
	
   public static void main(String[] args) throws IOException, InterruptedException, ScriptException, NoSuchMethodException {
     
     ConfigurableApplicationContext cac = SpringApplication.run(AnalyzerApplication.class, args);
     // Uncomment below for online mode of execution
	 //Analyzer bean = cac.getBean(Analyzer.class);
     //bean.analyzeMarkLogic();
   }
   
   @Bean
   @ConditionalOnProperty(name = "spring.config.location", matchIfMissing = false)
   public PropertiesConfiguration propertiesConfiguration(
     @Value("${spring.config.location}") String path) throws Exception {
       String filePath = new File(path.substring("file:".length())).getCanonicalPath();
       PropertiesConfiguration configuration = new PropertiesConfiguration(
         new File(filePath));
       FileChangedReloadingStrategy fileChangedReloadingStrategy = new FileChangedReloadingStrategy();
       fileChangedReloadingStrategy.setRefreshDelay(5000);
       configuration.setReloadingStrategy(fileChangedReloadingStrategy);
       return configuration;
   }
   
   @Bean
   @ConditionalOnBean(PropertiesConfiguration.class)
   @Primary
   public Properties properties(PropertiesConfiguration propertiesConfiguration) throws Exception {
       ReloadableProperties properties = new ReloadableProperties(propertiesConfiguration);
       return properties;
   }
   
   @Bean("asyncExecutor")
   public TaskExecutor getAsyncExecutor() {
	   ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
	   executor.setThreadNamePrefix("asyncExecutor-");
	   executor.setWaitForTasksToCompleteOnShutdown(true);
	   return executor;
   }
}