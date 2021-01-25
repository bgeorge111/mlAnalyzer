package com.marklogic.analyzer;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Future;

import javax.script.ScriptException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.marklogic.client.DatabaseClient;

@Service

public class AnalyzerService {
	@Autowired 
	@Lazy
    private RestTemplate restTemplate;
	@Autowired 
	@Lazy
	private Analyzer analyzer;
    @Autowired
    private StandardEnvironment environment;
    
	private final Logger logger = LoggerFactory.getLogger(AnalyzerService.class);
	
	@Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
	
	public String generateOutputUri () throws IOException, KeyManagementException, NoSuchAlgorithmException 
	{
		String ssl=environment.getProperty("marklogic.ssl");
		DatabaseClient client = null;
		if (ssl.equalsIgnoreCase("true")) {
		 client = analyzer.getSSLDatabaseClient();
		}
		else {
			client = analyzer.getDatabaseClient();
		}
			
		AnalyzerConfig config = analyzer.setupAnalyzerConfig(client);
		return config.getOutputUri();
	}
	
	@Async("asyncExecutor")
	public Future<String> invokeAnalyzer(RequestVO request) throws NoSuchMethodException, IOException, InterruptedException, ScriptException 
    {
		String outputUri = request.getOutputUri();
		String location = request.getScheme() + "://" + request.getHost() + ":" + request.getPort() + "/v1/InvokeAnalyzer?outputUri={outputUri}";
        restTemplate.postForLocation(location,null,outputUri); // Pass outputUri as parameter
		//return CompletableFuture.completedFuture("SUCCESS");
	    return new AsyncResult<>(outputUri);
    }

}
