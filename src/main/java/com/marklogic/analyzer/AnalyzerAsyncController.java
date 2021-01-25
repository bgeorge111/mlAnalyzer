package com.marklogic.analyzer;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import javax.script.ScriptException;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AnalyzerAsyncController {
	
	@Autowired
	@Lazy
    private AnalyzerService service;
	@Autowired
	private HttpServletRequest context;
	
	private final Logger logger = LoggerFactory.getLogger(AnalyzerService.class);

	@RequestMapping(value = "/v1/analyzer/invoke", method = RequestMethod.POST)
    public ResponseEntity<ResponseObject> invokeAnalyzer() throws InterruptedException, ExecutionException, NoSuchMethodException, IOException, ScriptException, KeyManagementException, NoSuchAlgorithmException 
    {
		String pattern = "yyyy-MM-dd HH:mm:ss.SSSZ";
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
		String outputUri = service.generateOutputUri();
		String description = "MarkLogic Data Analyzer Invoked. Wait for the URI(outPutUri) or file(outputFile) to be generated.";
		/*logger
		 * The service is Async and we cannot pass HttpServletRequest. So, the required information is put in a VO and passed. 
		 */
		RequestVO request = new RequestVO();
		request.setContext(context.getContextPath());
		request.setScheme(context.getScheme());
		request.setHost(context.getServerName());
		request.setPort(context.getServerPort());
		request.setOutputUri(outputUri);
		
		service.invokeAnalyzer(request);
		
		ResponseObject respObj = new ResponseObject();
		respObj.setOutputUri(outputUri);
		respObj.setOutputFile("out/"+outputUri.replaceAll("/", "_"));
		respObj.setProcessedTime(simpleDateFormat.format(new Date()));
		respObj.setDescription(description);
		respObj.setRequestURL(context.getRequestURL().toString());
		return ResponseEntity.ok(respObj);

    }

}
