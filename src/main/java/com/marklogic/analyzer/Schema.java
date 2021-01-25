package com.marklogic.analyzer;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
"name",
"schemaId",
"popularity",
"paths"
})
public class Schema {

@JsonProperty("name")
private String name;
@JsonProperty("collection")
private String collection;
@JsonProperty("schemaNumber")
private Integer schemaNumber;
@JsonProperty("schemaId")
private String schemaId;
@JsonProperty("popularity")
private Integer popularity;
@JsonProperty("paths")
private List<String> paths = null;
@JsonProperty("uri")
private String uri = null;
@JsonProperty("generatedSchemaUri")
private String generatedSchemaUri = null;
@JsonIgnore
private Map<String, Object> additionalProperties = new HashMap<String, Object>();

@JsonProperty("name")
public String getName() {
return name;
}

@JsonProperty("name")
public void setName(String name) {
this.name = name;
}

@JsonProperty("schemaId")
public String getSchemaId() {
return schemaId;
}

@JsonProperty("schemaId")
public void setSchemaId(String schemaId) {
this.schemaId = schemaId;
}

@JsonProperty("popularity")
public Integer getPopularity() {
return popularity;
}

@JsonProperty("popularity")
public void setPopularity(Integer popularity) {
this.popularity = popularity;
}

@JsonProperty("paths")
public List<String> getPaths() {
return paths;
}

@JsonProperty("paths")
public void setPaths(List<String> paths) {
this.paths = paths;
}

@JsonAnyGetter
public Map<String, Object> getAdditionalProperties() {
return this.additionalProperties;
}

@JsonAnySetter
public void setAdditionalProperty(String name, Object value) {
this.additionalProperties.put(name, value);
}

public Integer getSchemaNumber() {
	return schemaNumber;
}

public void setSchemaNumber(Integer schemaNumber) {
	this.schemaNumber = schemaNumber;
}

public String getCollection() {
	return collection;
}

public void setCollection(String collection) {
	this.collection = collection;
}

public String getUri() {
	return uri;
}

public void setUri(String uri) {
	this.uri = uri;
}

public String getGeneratedSchemaUri() {
	return generatedSchemaUri;
}

public void setGeneratedSchemaUri(String generatedSchemaUri) {
	this.generatedSchemaUri = generatedSchemaUri;
}



}
