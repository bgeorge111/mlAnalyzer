package com.marklogic.analyzer;

public class URI {
	String Uri;
	String[] collections;

	public void setUri(String uri) {
		this.Uri = uri;
	}

	public String getUri() {
		return Uri;
	}

	public void setCollections(String[] collections) {
		this.collections = collections;
	}

	public String[] getCollections() {
		return collections;
	}
}
