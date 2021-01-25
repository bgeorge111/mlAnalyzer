package com.marklogic.analyzer;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClient.ConnectionType;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.DatabaseClientFactory.SSLHostnameVerifier;
import com.marklogic.client.datamovement.DataMovementManager;
import com.marklogic.client.datamovement.ExportListener;
import com.marklogic.client.datamovement.JobTicket;
import com.marklogic.client.datamovement.ProgressListener;
import com.marklogic.client.datamovement.QueryBatcher;
import com.marklogic.client.document.DocumentManager;
import com.marklogic.client.document.DocumentManager.Metadata;
import com.marklogic.client.document.DocumentPage;
import com.marklogic.client.document.DocumentRecord;
import com.marklogic.client.document.JSONDocumentManager;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.DocumentMetadataHandle.Capability;
import com.marklogic.client.io.DocumentMetadataHandle.DocumentPermissions;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.InputStreamHandle;
import com.marklogic.client.io.StringHandle;
import com.marklogic.client.query.QueryManager;
import com.marklogic.client.query.RawCtsQueryDefinition;
import com.marklogic.client.query.StructuredQueryBuilder;
import com.marklogic.client.query.StructuredQueryDefinition;

@Component
//@PropertySource(value = { "classpath:user.properties" }, ignoreResourceNotFound = true)
public class Analyzer {
	@Autowired
	private StandardEnvironment environment;

	@Value("${marklogic.host}")
	private String host;
	@Value("${marklogic.port}")
	private int port;
	@Value("${marklogic.username}")
	private String username;
	@Value("${marklogic.password}")
	private String password;
	@Value("${marklogic.database}")
	private String database;
	@Value("${marklogic.auth}")
	private String auth;
	@Value("${marklogic.ssl}")
	private String ssl;
	@Value("${analyzer.collections}")
	private String collections;
	@Value("${logLevel}")
	private String LOGLEVEL;
	@Value("${analyzer.config}")
	private String ANALYZER_CONFIG;
	@Value("${analyzer.output-type}")
	private String OUTPUT_TYPE;
	@Value("${analyzer.schema-generate}")
	private String SCHEMA_GENERATE;

	private int batchsize = 1;
	private int threads = 1;
	private ConcurrentHashMap<String, Integer> hmPath = new ConcurrentHashMap<String, Integer>();
	private ConcurrentHashMap<String, Integer> hmAttr = new ConcurrentHashMap<String, Integer>();
	private ConcurrentHashMap<String, List<URI>> hmURI = new ConcurrentHashMap<String, List<URI>>();
	private ConcurrentHashMap<String, List<Schema>> hmCollection = new ConcurrentHashMap<String, List<Schema>>();
	/* Atomic integers used to be thread safe */
	AtomicInteger xmlDocCount = new AtomicInteger(0);
	AtomicInteger jsonDocCount = new AtomicInteger(0);
	AtomicInteger binDocCount = new AtomicInteger(0);
	AtomicInteger txtDocCount = new AtomicInteger(0);
	AtomicInteger otherDocCount = new AtomicInteger(0);

	private final Logger logger = LoggerFactory.getLogger(Analyzer.class);

	String readFile(String fileName) throws IOException {
		InputStream resource = new ClassPathResource(fileName).getInputStream();
		BufferedReader br = new BufferedReader(new InputStreamReader(resource));
		try {
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();

			while (line != null) {
				sb.append(line);
				sb.append("\n");
				line = br.readLine();
			}
			return sb.toString();
		} finally {
			br.close();
		}
	}

	@Bean
	public DatabaseClient getSSLDatabaseClient() throws KeyManagementException, NoSuchAlgorithmException {
		TrustManager naiveTrustMgr[] = new X509TrustManager[] { new X509TrustManager() {

			@Override
			public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				// TODO Auto-generated method stub

			}

			@Override
			public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				// TODO Auto-generated method stub

			}

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				// TODO Auto-generated method stub
				return new X509Certificate[0];
			}
		} };

		SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
		sslContext.init(null, naiveTrustMgr, null);

		DatabaseClient client = null;

		if (auth.compareToIgnoreCase("BASIC") == 0) {
			client = DatabaseClientFactory.newClient(host, port, database,
					new DatabaseClientFactory.BasicAuthContext(username, password)
							.withSSLContext(sslContext, (X509TrustManager) naiveTrustMgr[0])
							.withSSLHostnameVerifier(SSLHostnameVerifier.ANY),
					ConnectionType.GATEWAY);
		}
		if (auth.compareToIgnoreCase("DIGEST") == 0) {
			client = DatabaseClientFactory.newClient(host, port, database,
					new DatabaseClientFactory.DigestAuthContext(username, password)
							.withSSLContext(sslContext, (X509TrustManager) naiveTrustMgr[0])
							.withSSLHostnameVerifier(SSLHostnameVerifier.ANY),
					ConnectionType.GATEWAY);
		}

		return client;
	}

	public DatabaseClient getDatabaseClient() {
		DatabaseClient client = null;

		if (auth.compareToIgnoreCase("BASIC") == 0) {
			client = DatabaseClientFactory.newClient(host, port, database,
					new DatabaseClientFactory.BasicAuthContext(username, password), ConnectionType.DIRECT);
		}
		if (auth.compareToIgnoreCase("DIGEST") == 0) {
			client = DatabaseClientFactory.newClient(host, port, database,
					new DatabaseClientFactory.DigestAuthContext(username, password), ConnectionType.DIRECT);
		}

		return client;
	}

	@Bean
	public QueryManager getQueryManager() throws KeyManagementException, NoSuchAlgorithmException {
		return getDatabaseClient().newQueryManager();
	}

	@Bean
	public JSONDocumentManager getJSONDocumentManager() throws KeyManagementException, NoSuchAlgorithmException {
		return getDatabaseClient().newJSONDocumentManager();
	}

	public String MD5(String md5) {
		try {
			java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
			byte[] array = md.digest(md5.getBytes());
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < array.length; ++i) {
				sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
			}
			return sb.toString();
		} catch (java.security.NoSuchAlgorithmException e) {
		}
		return null;
	}

	public boolean containsId(final List<Schema> list, final String id) {
		return list.stream().filter(o -> o.getSchemaId().equals(id)).findFirst().isPresent();
	}

	private String outputAsString(String title, String description, String json, JsonNodeType type) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode jsonNode = mapper.readTree(json);
		StringBuilder output = new StringBuilder();
		output.append("{");

		if (type == null)
			output.append("\"$schema\": \"http://json-schema.org/draft-04/schema#\"," + "\"title\": \"" + title
					+ "\", \"description\": \"" + description + "\", \"type\": \"object\", \"properties\": {");

		for (Iterator<String> iterator = jsonNode.fieldNames(); iterator.hasNext();) {
			String fieldName = iterator.next();

			JsonNodeType nodeType = jsonNode.get(fieldName).getNodeType();

			output.append(convertNodeToStringSchemaNode(jsonNode, nodeType, fieldName));
		}

		if (type == null)
			output.append("}");

		output.append("}");
		/*
		 * For an observed error with handling array types, a replacement of blank
		 * objects is done. Replace all },} with }}
		 */
		return output.toString().replaceAll("},}", "}}");
	}

	private String convertNodeToStringSchemaNode(JsonNode jsonNode, JsonNodeType nodeType, String key)
			throws IOException {
		StringBuilder result = new StringBuilder("\"" + key + "\": { \"type\": \"");
		JsonNode node = null;
		switch (nodeType) {
		case ARRAY:
			node = jsonNode.get(key).get(0);
			if (node != null) {
				result.append("array\", \"items\": { \"properties\":");
				result.append(outputAsString(null, null, node.toString(), JsonNodeType.ARRAY));
				result.append("}},");
			} else {
				result.append("array\" },");
			}

			break;
		case BOOLEAN:
			result.append("boolean\" },");
			break;
		case NUMBER:
			result.append("number\" },");
			break;
		case OBJECT:
			node = jsonNode.get(key);
			result.append("object\", \"properties\": ");
			result.append(outputAsString(null, null, node.toString(), JsonNodeType.OBJECT));
			result.append("},");
			break;
		case STRING:
			result.append("string\" },");
			break;
		case NULL:
			result.append("null\" },");
			break;

		}

		return result.toString();
	}

	public String[] generateSchema(String uri, DatabaseClient client) throws IOException {
		StructuredQueryBuilder sb = new StructuredQueryBuilder();
		StructuredQueryDefinition criteria = sb.document(uri);
		DocumentManager docMgr = client.newDocumentManager();
		DocumentPage page = docMgr.search(criteria, 1);
		DocumentRecord record = page.next();
		InputStream byteStream = record.getContent(new InputStreamHandle()).get();
		String[] result = new String[2];
		if (Format.XML.equals(record.getFormat())) {
			XsdGen xsdgen = new XsdGen();
			result[0] = "XSD";
			result[1] = xsdgen.generateSchemaText(byteStream);
			return result;
		} else if (Format.JSON.equals(record.getFormat())) {

			String str = IOUtils.toString(byteStream, StandardCharsets.UTF_8);
			result[0] = "JSON";
			result[1] = outputAsString("schemaTitle", "Schema generated by Analyzer", str, null);
			return result;
		} else {
			result[0] = "UNKNOWN";
			result[1] = "";
			return result;
		}
	}

	public synchronized void updateMap(List<String> paths, String[] intersection, String uri) {
		// Save the path array to collectionMap for each collection that the doc belongs
		// to
		for (int i = 0; i < intersection.length; i++) {
			String schemaId = MD5(paths.toString());
			Schema schema = new Schema();
			schema.setSchemaId(schemaId);
			schema.setPaths(paths);
			List<Schema> lstSchema = null;
			lstSchema = hmCollection.get(intersection[i]);

			// if the schemaId is already found, do not add to the collection map as we need
			// only one instance of the unique path.
			if (lstSchema == null) {
				lstSchema = hmCollection.get(intersection[i]);
				if (lstSchema == null) {
					schema.setPopularity(1);
					schema.setCollection(intersection[i]);
					schema.setName(intersection[i] + "-schema-" + "1");
					schema.setSchemaNumber(1);
					schema.setUri(uri);
					lstSchema = new ArrayList<Schema>();
					lstSchema.add(schema);
				}
			} else if (!containsId(lstSchema, schemaId)) {
				// Find the highest schema number for the collection
				schema.setPopularity(1);
				schema.setCollection(intersection[i]);
				Integer latestNumber = 0;

				for (Schema tmpSchema : lstSchema) {
					Integer tmpNumber = 0;
					if (tmpSchema != null && intersection[i].equals(tmpSchema.getCollection())) {
						tmpNumber = tmpSchema.getSchemaNumber();
						if (tmpNumber > latestNumber)
							latestNumber = tmpNumber;
					}
				}
				schema.setSchemaNumber(latestNumber + 1);
				schema.setUri(uri);
				schema.setName(intersection[i] + "-schema-" + String.valueOf(latestNumber + 1));
				lstSchema.add(schema);

			} else {
				// get the existing matching schema to update the popularity.
				for (Schema tmpSchema : lstSchema) {
					if (tmpSchema != null && schemaId.equals(tmpSchema.getSchemaId())) {
						Integer currentPopularity = tmpSchema.getPopularity();
						tmpSchema.setPopularity(currentPopularity + 1);
						break;
					}
				}
			}
			hmCollection.put(intersection[i], lstSchema);
		}
	}

	public synchronized void updateCollectionMap(DocumentRecord doc, Object[] targetCollectionObjects,
			Object[] docCollectionObjects) throws Exception {
		String[] docCollections = Arrays.copyOf(docCollectionObjects, docCollectionObjects.length, String[].class);
		String[] targetCollections = Arrays.copyOf(targetCollectionObjects, targetCollectionObjects.length,
				String[].class);

		HashSet<String> set = new HashSet<>();
		set.addAll(Arrays.asList(docCollections));
		set.retainAll(Arrays.asList(targetCollections));
		String[] intersection = {};
		intersection = set.toArray(intersection);

		Format format = doc.getFormat();
		String uri = doc.getUri();
		StringHandle handle = doc.getContent(new StringHandle());
		if (Format.JSON.equals(format)) {
			jsonDocCount.addAndGet(1);
			JsonParser json = new JsonParser(handle.get());
			List<String> paths = json.getPathList();
			paths = paths.stream().distinct().collect(Collectors.toList());
			updateMap(paths, intersection, uri);
		} else if (Format.XML.equals(format)) {
			xmlDocCount.addAndGet(1);
			XMLParser xml = new XMLParser();
			InputStream stream = new ByteArrayInputStream(handle.get().getBytes(StandardCharsets.UTF_8));
			try {
				List<String> paths = xml.pathList(stream);
				paths = paths.stream().distinct().collect(Collectors.toList());
				updateMap(paths, intersection, uri);
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

	}

	public synchronized void updatePathMap(DocumentRecord doc, Integer docCount) throws XPathExpressionException {
		Format format = doc.getFormat();
		String uri = doc.getUri();
		StringHandle handle = doc.getContent(new StringHandle());
		DocumentMetadataHandle metadata = new DocumentMetadataHandle();

		if (Format.JSON.equals(format)) {
			jsonDocCount.addAndGet(1);
			JSONObject json = new JSONObject(handle.get());
			performJSONAnalysis(json, uri, docCount);
			json = null;
		} else if (Format.XML.equals(format)) {
			xmlDocCount.addAndGet(1);
			DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = null;
			try {
				builder = builderFactory.newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			}

			InputStream stream = new ByteArrayInputStream(handle.get().getBytes(StandardCharsets.UTF_8));
			try {
				Document document = builder.parse(stream);
				performXMLAnalysis(document.getDocumentElement(), new StringBuilder(), uri, docCount);
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else if (Format.BINARY.equals(format)) {
			binDocCount.addAndGet(1);
		} else if (Format.TEXT.equals(format)) {
			txtDocCount.addAndGet(1);
		} else {
			otherDocCount.addAndGet(1);
		}

		return;
	}

	private void performJSONAnalysis(JSONObject json, String uri, Integer docCount) {
		listJSONObject("", json, uri, docCount);
	}

	private void listObject(String parent, Object data, String uri, Integer docCount) {
		if (data instanceof JSONObject) {
			listJSONObject(parent, (JSONObject) data, uri, docCount);
		} else if (data instanceof JSONArray) {
			listJSONArray(parent, (JSONArray) data, uri, docCount);
		} else {
			listPrimitive(parent, data, uri, docCount);
		}
	}

	private void listJSONObject(String parent, JSONObject json, String uri, Integer docCount) {
		Iterator<?> it = json.keys();
		while (it.hasNext()) {
			String key = (String) it.next();
			Object child = json.get(key);
			String childKey = parent.isEmpty() ? key : parent + "." + key;
			listObject(childKey, child, uri, docCount);
		}
	}

	private void listJSONArray(String parent, JSONArray json, String uri, Integer docCount) {
		for (int i = 0; i < json.length(); i++) {
			Object data = json.get(i);
			listObject(parent + "(*)", data, uri, docCount);
		}
	}

	private void listPrimitive(String parent, Object obj, String uri, Integer docCount) {
		String normalizedParent = parent.replaceAll("\\[(.*?)\\]", "").replace(".", "/");
		if (hmPath.containsKey(normalizedParent)) {
			Integer val = hmPath.get(normalizedParent) + 1;
			hmPath.put(normalizedParent, val);
			if (hmURI.containsKey(normalizedParent) && hmURI.get(normalizedParent).size() < docCount) {
				URI objURI = new URI();
				objURI.setUri(uri);
				ArrayList<URI> uris = (ArrayList<URI>) hmURI.get(normalizedParent);
				uris.add(objURI);
				hmURI.put(normalizedParent, uris);
			}
		} else {
			hmPath.put(normalizedParent.toString(), 1);
			URI objURI = new URI();
			objURI.setUri(uri);
			ArrayList<URI> uris = new ArrayList<URI>();
			uris.add(objURI);
			hmURI.put(normalizedParent, uris);
		}
	}

	private void performXMLAnalysis(Element elem, StringBuilder path, String uri, Integer docCount) {
		final int pathLen = path.length();
		if (pathLen != 0)
			path.append("/");
		path.append(elem.getNodeName());
		NamedNodeMap nmap = elem.getAttributes();
		for (int i = 0; i < nmap.getLength(); i++) {
			String key = nmap.item(i).getNodeName();
			if (hmAttr.containsKey(key)) {
				Integer val = hmAttr.get(key) + 1;
				hmAttr.put(key, val);
			} else {
				hmAttr.put(key, 1);
			}
		}

		boolean hasChild = false;
		for (Node child = elem.getFirstChild(); child != null; child = child.getNextSibling())
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				hasChild = true;
				performXMLAnalysis((Element) child, path, uri, docCount);
			}
		if (!hasChild) {
			if (hmPath.containsKey(path.toString())) {
				Integer val = hmPath.get(path.toString()) + 1;
				hmPath.put(path.toString(), val);
				if (hmURI.containsKey(path.toString()) && hmURI.get(path.toString()).size() < docCount) {
					URI objURI = new URI();
					objURI.setUri(uri);
					ArrayList<URI> uris = (ArrayList<URI>) hmURI.get(path.toString());
					uris.add(objURI);
					hmURI.put(path.toString(), uris);
				}
			} else {
				hmPath.put(path.toString(), 1);
				URI objURI = new URI();
				objURI.setUri(uri);
				ArrayList<URI> uris = new ArrayList<URI>();
				uris.add(objURI);
				hmURI.put(path.toString(), uris);
			}
		}
		path.setLength(pathLen);
	}

	public long performAnalysis(DatabaseClient client, AnalyzerConfig config, String... collections)
			throws InterruptedException {

		DataMovementManager dmvMgr = client.newDataMovementManager();
		QueryManager queryMgr = client.newQueryManager();
		StringHandle handle = new StringHandle(config.getQuery()).withFormat(Format.JSON);
		RawCtsQueryDefinition query = queryMgr.newRawCtsQueryDefinition(handle);
		List<String> combinedCollections = new ArrayList<String>(Arrays.asList(collections));
		combinedCollections.addAll(Arrays.asList(config.getCollections()));
		query.setCollections((String[]) combinedCollections.toArray(new String[0]));
		Integer docCount = config.getDocCount();
		batchsize = config.getBatchSize();
		threads = config.getThreads();
		if (batchsize <= 0) {
			batchsize = 1;
		}
		if (threads <= 0) {
			threads = 1;
		}

		final QueryBatcher batcher = dmvMgr.newQueryBatcher(query).withBatchSize(batchsize).withThreadCount(threads)
				.onUrisReady(new ExportListener().withMetadataCategory(Metadata.COLLECTIONS)
						.withNonDocumentFormat(Format.XML).onDocumentReady(doc -> {
							try {
								if (OUTPUT_TYPE.equalsIgnoreCase("PATHS")) {
									updatePathMap(doc, docCount);
								} else {
									DocumentMetadataHandle metadata = doc.getMetadata(new DocumentMetadataHandle());
									updateCollectionMap(doc, combinedCollections.toArray(),
											metadata.getCollections().toArray());
								}
								doc = null;
							} catch (Exception e) {
								e.printStackTrace();
							}
						}))
				.onUrisReady(new ProgressListener().onProgressUpdate(progressUpdate -> {
					logger.warn(progressUpdate.getProgressAsString());
				})).withJobName("Analyzer Data Movement Job").onQueryFailure(exception -> exception.printStackTrace())
				.withConsistentSnapshot();
		final JobTicket ticket = dmvMgr.startJob(batcher);
		logger.warn("Data Movement Job --> " + ticket.getJobId() + " Started");
		batcher.awaitCompletion();
		// batcher.awaitCompletion(1L, TimeUnit.MINUTES);
		dmvMgr.stopJob(ticket);
		logger.warn("Data Movement Job --> " + ticket.getJobId() + " Ended");
		return 0L;
	}

	public AnalyzerConfig setupAnalyzerConfig(DatabaseClient client) throws IOException {
		// create a manager for JSON documents
		JSONDocumentManager docMgr = client.newJSONDocumentManager();
		AnalyzerConfig config = new AnalyzerConfig();
		ObjectMapper mapper = new ObjectMapper();
		Date outputTime = new Date();
		// create a handle to receive the document content
		StringHandle handle = new StringHandle();

		docMgr.read(ANALYZER_CONFIG, handle);
		JsonNode jsonNode = mapper.readTree(handle.get());
		String finalUri = jsonNode.get("taskConfig").get("uriPrefix").textValue() + outputTime.getTime() + ".json";
		config.setQuery("{'ctsquery':" + jsonNode.get("taskConfig").get("query").toString() + "}");
		config.setDocCount(jsonNode.get("taskConfig").get("docCount").intValue());
		config.setUriPrefix(jsonNode.get("taskConfig").get("uriPrefix").textValue());
		config.setBatchSize(jsonNode.get("taskConfig").get("batchsize").intValue());
		config.setThreads(jsonNode.get("taskConfig").get("threads").intValue());
		config.setCollections(jsonNode.get("taskConfig").get("collections").textValue().split(","));
		config.setOutputCollections(jsonNode.get("taskConfig").get("outputCollections").textValue().split(","));
		config.setOutputPermissions(jsonNode.get("taskConfig").get("outputPermissions").textValue());
		config.setOutputTime(outputTime);
		config.setOutputUri(finalUri);

		return config;
	}

	public void writeSchema(DatabaseClient client, String schemaString, String schemaUri, AnalyzerConfig config) {
		JSONDocumentManager docMgr = client.newJSONDocumentManager();
		DocumentMetadataHandle metadata = new DocumentMetadataHandle();
		DocumentPermissions permissions = metadata.getPermissions();
		parsePermissions(config.getOutputPermissions().trim().split("\\s*,\\s*"), permissions);
		metadata.setPermissions(permissions);
		metadata.getCollections().addAll(config.getOutputCollections());
		logger.warn("Writing Schema Document " + schemaUri);
		StringHandle handle = new StringHandle(schemaString);
		docMgr.write(schemaUri, metadata, handle);
		logger.warn("Wrote Schema Document " + schemaUri);
		FileWriter schemaFileWriter = null;
		try {
			schemaFileWriter = new FileWriter("out/" + schemaUri.replaceAll("/", "_"));
			schemaFileWriter.write(schemaString);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (schemaFileWriter != null) {
					schemaFileWriter.flush();
					schemaFileWriter.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void createAnalysisDocument(DatabaseClient client, AnalyzerConfig config, long processDuration)
			throws ScriptException, IOException, NoSuchMethodException {

		String pattern = "yyyy-MM-dd HH:mm:ss.SSSZ";
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
		ObjectMapper mapper = new ObjectMapper();
		String schemaUri = "";
		String schemaString = "";
		ObjectNode rootNode = mapper.createObjectNode();
		ObjectNode analysisNode = mapper.createObjectNode();
		String analysisDesc = "This lists the unique paths & attributes in matching documents(only leaf nodes).If configured, a random sample list of "
				+ "URIs matching the path and the collections wherethese URIs belong are provided.";
		analysisNode.put("description", analysisDesc);
		if (OUTPUT_TYPE.equalsIgnoreCase("PATHS")) {
			ArrayNode pathArrayNode = mapper.createArrayNode();
			Iterator<Map.Entry<String, Integer>> itr = hmPath.entrySet().iterator();
			while (itr.hasNext()) {
				Map.Entry<String, Integer> entry = itr.next();
				Integer popularity = entry.getValue();
				String path = entry.getKey();
				ObjectNode pathNode = mapper.createObjectNode();
				pathNode.put("path", path);
				pathNode.put("popularity", popularity);
				ArrayNode uriArrayNode = mapper.createArrayNode();
				List<URI> lstURI = hmURI.get(path);
				for (int i = 0; i < lstURI.size(); i++) {
					ObjectNode uriNode = mapper.createObjectNode();
					uriNode.put("uri", lstURI.get(i).getUri());
					uriArrayNode.add(uriNode);
				}
				pathNode.putArray("uris").addAll(uriArrayNode);
				pathArrayNode.add(pathNode);
			}
			analysisNode.putArray("paths").addAll(pathArrayNode);
		} else {
			ArrayNode schemaArrayNode = mapper.createArrayNode();
			Iterator<Map.Entry<String, List<Schema>>> itr = hmCollection.entrySet().iterator();
			ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
			String script = readFile("structurize.js");
			while (itr.hasNext()) {
				Map.Entry<String, List<Schema>> entry = itr.next();
				String collection = entry.getKey();
				ObjectNode pathNode = mapper.createObjectNode();
				pathNode.put("name", collection);
				schemaArrayNode.add(pathNode);
				ArrayNode schemaArrays = mapper.createArrayNode();
				List<Schema> lstSchema = entry.getValue();
				for (int i = 0; i < lstSchema.size(); i++) {
					ObjectNode schema = mapper.createObjectNode();
					ArrayNode array = mapper.valueToTree(lstSchema.get(i).getPaths());
					schema.put("name", lstSchema.get(i).getName());
					schema.put("id", lstSchema.get(i).getSchemaId());
					ObjectNode definitionNode = mapper.createObjectNode();
					definitionNode.put("title", "");
					definitionNode.put("type", "object");
					/*
					 * The below block of code converts the path structure to a tree structure. This
					 * is done using ScriptEngine. This could be done with native Java code and less
					 * complex.nashhorn is used for more academic interest. Feel free to change.
					 */

					try {
						if (OUTPUT_TYPE.equalsIgnoreCase("STRUCTURIZED_SCHEMA")) {
							String strPath = mapper.writeValueAsString(array);
							engine.put("strPath", strPath);
							Object obj = engine.eval("JSON.parse(strPath)");
							engine.eval(script);
							Invocable invocable = (Invocable) engine;
							String result = (String) invocable.invokeFunction("invoke", obj);
							JsonNode actualObj = mapper.readTree(result);
							definitionNode.put("properties", actualObj.get(0));
						} else {
							definitionNode.put("properties", array);
						}

					} catch (ScriptException ex) {
						ex.printStackTrace();
					}
					schema.put("popularity", lstSchema.get(i).getPopularity());
					schema.put("definition", definitionNode);
					if (SCHEMA_GENERATE.equalsIgnoreCase("TRUE") || SCHEMA_GENERATE.equalsIgnoreCase("YES")) {
						String[] generatedSchemaResults = generateSchema(lstSchema.get(i).getUri(), client);
						schemaString = generatedSchemaResults[1];
						if ("XSD".equalsIgnoreCase(generatedSchemaResults[0])) {
							schemaUri = config.getUriPrefix() + config.getOutputTime().getTime() + "_"
									+ lstSchema.get(i).getName() + ".xsd";
							/*
							 * Assuming the best and writing the schema here itself.
							 */
							writeSchema(client, schemaString, schemaUri, config);
						} else if ("JSON".equalsIgnoreCase(generatedSchemaResults[0])) {
							schemaUri = config.getUriPrefix() + config.getOutputTime().getTime() + "_"
									+ lstSchema.get(i).getName() + ".json";
							/*
							 * Assuming the best and writing the schema here itself.
							 */
							writeSchema(client, schemaString, schemaUri, config);
						}
					}
					ObjectNode exampleNode = mapper.createObjectNode();
					exampleNode.put("uri", lstSchema.get(i).getUri());
					exampleNode.put("schemaUri", schemaUri);
					schema.put("example", exampleNode);
					schemaArrays.add(schema);
				}

				pathNode.putArray("schemata").addAll(schemaArrays);
			}
			analysisNode.putArray("paths").addAll(schemaArrayNode);
		}

		ArrayNode attrArrayNode = mapper.createArrayNode();
		Iterator<Map.Entry<String, Integer>> aitr = hmAttr.entrySet().iterator();
		while (aitr.hasNext()) {
			Map.Entry<String, Integer> entry = aitr.next();
			Integer popularity = entry.getValue();
			String attr = entry.getKey();
			ObjectNode attrNode = mapper.createObjectNode();
			attrNode.put("attribute", attr);
			attrNode.put("popularity", popularity);
			attrArrayNode.add(attrNode);
		}
		analysisNode.putArray("attributes").addAll(attrArrayNode);
		analysisNode.put("totalPaths", hmPath.size());
		analysisNode.put("totalAttributes", hmAttr.size());
		// rootNode.put("analysis", analysisNode);
		rootNode.set("analysis", analysisNode);

		ObjectNode typeStatsNode = mapper.createObjectNode();
		String typeStatsDesc = "This provides what are the kind of documents available in the database that being analysed.The query constraint is not used here. "
				+ "This is the document count information in the target database and collections.";
		typeStatsNode.put("description", typeStatsDesc);
		typeStatsNode.put("xmlDocCount", xmlDocCount.get());
		typeStatsNode.put("jsonDocCount", jsonDocCount.get());
		typeStatsNode.put("binDocCount", binDocCount.get());
		typeStatsNode.put("textDocCount", txtDocCount.get());
		typeStatsNode.put("totalDocs",
				(xmlDocCount.get() + jsonDocCount.get() + binDocCount.get() + txtDocCount.get()));

		rootNode.set("typeStats", typeStatsNode);

		ObjectNode auditNode = mapper.createObjectNode();
		String auditDesc = "This has audit information about when this analysis document was created, which databasewas analysed under which id and which query was used.";
		auditNode.put("description", auditDesc);
		auditNode.put("database", database);
		auditNode.put("timestamp", simpleDateFormat.format(config.getOutputTime()));
		auditNode.put("runTime", (processDuration / 60));
		auditNode.put("query", config.getQuery());

		rootNode.set("audit", auditNode);

		JSONDocumentManager docMgr = client.newJSONDocumentManager();
		DocumentMetadataHandle metadata = new DocumentMetadataHandle();
		metadata.getCollections().addAll(config.getOutputCollections());
		DocumentPermissions permissions = metadata.getPermissions();
		parsePermissions(config.getOutputPermissions().trim().split("\\s*,\\s*"), permissions);
		metadata.setPermissions(permissions);
		StringHandle handle = new StringHandle(mapper.writeValueAsString(rootNode));
		String finalUri = config.getOutputUri();
		logger.warn("Writing Analysis Document " + finalUri);
		docMgr.write(finalUri, metadata, handle);
		logger.warn("Wrote Analysis Document " + finalUri);
		FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter("out/" + finalUri.replaceAll("/", "_"));
			fileWriter.write(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (fileWriter != null) {
					fileWriter.flush();
					fileWriter.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return;
	}

	public void cleanUp() {
		hmPath.clear();
		hmAttr.clear();
		hmURI.clear();
		hmCollection.clear();
		xmlDocCount.set(0);
		jsonDocCount.set(0);
		binDocCount.set(0);
		txtDocCount.set(0);
		otherDocCount.set(0);
	}

	public void initializeAnalyzerVariables() {
		/*
		 * To enable dynamic loading of properties, instead of @Value, the variables are
		 * initialized from Environment
		 */
		host = environment.getProperty("marklogic.host");
		port = Integer.parseInt(environment.getProperty("marklogic.port"));
		username = environment.getProperty("marklogic.username");
		password = environment.getProperty("marklogic.password");
		database = environment.getProperty("marklogic.database");
		auth = environment.getProperty("marklogic.auth");
		ssl = environment.getProperty("marklogic.ssl");
		collections = environment.getProperty("analyzer.collections");
		LOGLEVEL = environment.getProperty("logLevel");
		ANALYZER_CONFIG = environment.getProperty("analyzer.config");
		OUTPUT_TYPE = environment.getProperty("analyzer.output-type");
		SCHEMA_GENERATE = environment.getProperty("analyzer.schema-generate");
	}

	public void analyzeMarkLogic(String outputUri) throws IOException, InterruptedException, ScriptException,
			NoSuchMethodException, KeyManagementException, NoSuchAlgorithmException {
		String className = this.getClass().getName();
		Date start = new Date();

		logger.warn("Loading Configuration Parameters ");
		initializeAnalyzerVariables();
		logger.warn(" Started Analyzing with PID" + className + " " + ProcessHandle.current().pid());
		DatabaseClient client = null;
		if (ssl.equalsIgnoreCase("true")) {
			client = getSSLDatabaseClient();
		} else {
			client = getDatabaseClient();
		}

		AnalyzerConfig config = setupAnalyzerConfig(client);

		if (!outputUri.isEmpty()) {
			config.setOutputUri(outputUri);
		}
		String[] arrCollections = collections.split(",");
		performAnalysis(client, config, arrCollections);
		Date processTime = new Date();
		long processDuration = ((processTime.getTime() - start.getTime()) / 1000);
		createAnalysisDocument(client, config, processDuration);
		Date end = new Date();
		logger.warn(" Ended Analyzing " + className, LOGLEVEL);
		logger.warn("Execution time for " + className + " is " + (end.getTime() - start.getTime()) / 1000 + " seconds.",
				LOGLEVEL);
		cleanUp();
		return;

	}

	public void parsePermissions(String[] tokens, DocumentPermissions permissions) {
		for (int i = 0; i < tokens.length; i += 2) {
			String role = tokens[i];
			if (i + 1 >= tokens.length) {
				throw new IllegalArgumentException(
						"Unable to parse permissions string, which must be a comma-separated "
								+ "list of role names and capabilities - i.e. role1,read,role2,update,role3,execute; string: ");
			}
			String capability = tokens[i + 1];
			Capability c = null;
			if (capability.equals("execute")) {
				c = Capability.EXECUTE;
			} else if (capability.equals("insert")) {
				c = Capability.INSERT;
			} else if (capability.equals("update")) {
				c = Capability.UPDATE;
			} else if (capability.equals("read")) {
				c = Capability.READ;
			}
			if (permissions.containsKey(role)) {
				permissions.get(role).add(c);
			} else {
				permissions.add(role, c);
			}
		}

	}
}