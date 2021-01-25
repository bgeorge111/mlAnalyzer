package com.marklogic.analyzer;

public class RequestVO {
	
	String scheme;
	String host;
	int port;
	String context;
	String outputUri;
	public String getScheme() {
		return scheme;
	}
	public void setScheme(String scheme) {
		this.scheme = scheme;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public String getContext() {
		return context;
	}
	public void setContext(String context) {
		this.context = context;
	}
	public String getOutputUri() {
		return outputUri;
	}
	public void setOutputUri(String outputUri) {
		this.outputUri = outputUri;
	}

}
