package com.marklogic.analyzer;

import java.util.Date;

public class AnalyzerConfig {
	String query;
	Integer limit;
	Integer docCount;
	String [] collections;
	String [] outputCollections;
	String outputPermissions;
	String uriPrefix;
	Integer batchSize;
	Integer threads;
	String outputUri;
	Date outputTime;
	
	public String[] getCollections() {
		return collections;
	}

	public void setCollections(String[] collections) {
		this.collections = collections;
	}

	public String[] getOutputCollections() {
		return outputCollections;
	}

	public void setOutputCollections(String[] outputCollections) {
		this.outputCollections = outputCollections;
	}

	public String getUriPrefix() {
		return uriPrefix;
	}

	public void setUriPrefix(String uriPrefix) {
		this.uriPrefix = uriPrefix;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public String getQuery() {
		return query;
	}

	public void setLimit(Integer limit) {
		this.limit = limit;
	}

	public Integer getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(Integer batchSize) {
		this.batchSize = batchSize;
	}

	public Integer getThreads() {
		return threads;
	}

	public void setThreads(Integer threads) {
		this.threads = threads;
	}

	public Integer getLimit() {
		return limit;
	}

	public void setDocCount(Integer docCount) {
		this.docCount = docCount;
	}

	public Integer getDocCount() {
		return docCount;
	}

	public String getOutputUri() {
		return outputUri;
	}

	public void setOutputUri(String outputUri) {
		this.outputUri = outputUri;
	}

	public Date getOutputTime() {
		return outputTime;
	}

	public void setOutputTime(Date outputTime) {
		this.outputTime = outputTime;
	}

	public final String getOutputPermissions() {
		return outputPermissions;
	}

	public final void setOutputPermissions(String outputPermissions) {
		this.outputPermissions = outputPermissions;
	}
	
	
}