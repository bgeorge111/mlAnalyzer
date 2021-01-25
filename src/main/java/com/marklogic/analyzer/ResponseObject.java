package com.marklogic.analyzer;

public class ResponseObject {

	String outputUri ; 
	String outputFile ;
	String processedTime ;
	String description ;
	String requestURL;
	public String getOutputUri() {
		return outputUri;
	}
	public void setOutputUri(String outputUri) {
		this.outputUri = outputUri;
	}
	public String getOutputFile() {
		return outputFile;
	}
	public void setOutputFile(String outputFile) {
		this.outputFile = outputFile;
	}
	public String getProcessedTime() {
		return processedTime;
	}
	public void setProcessedTime(String processedTime) {
		this.processedTime = processedTime;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getRequestURL() {
		return requestURL;
	}
	public void setRequestURL(String requestURL) {
		this.requestURL = requestURL;
	}
	
}
