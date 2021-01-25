package com.marklogic.analyzer;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.script.ScriptException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AnalyzerRestController {
	
	@Autowired
	private Analyzer analyzer;
	
	  @RequestMapping(value = "/v1/InvokeAnalyzer", method = RequestMethod.POST)
	  public void invokeAnalyzer(@RequestParam String outputUri) throws NoSuchMethodException, IOException, InterruptedException, ScriptException, KeyManagementException, NoSuchAlgorithmException {
		  
		  analyzer.analyzeMarkLogic(outputUri); // Pass parameter here
		  
	  }

}
