# mlAnalyzer
A document analyzer for MarkLogic Databases. Can be used for data cataloging, reverse schema generation etc
Capabilities 
1. Configuraable cts query to analyze 
2. Generates the high level stats for all types of documents satisfying the query configured 
3. Generates the unique paths in the json and xml documents satisfying the query configured 
4. Generates the popularity of the unique paths in the json and xml documents satisfying the query configured 
5. Generates the unique json schema and xsd schema for json and xml documents satisfying the query configured. 
6. Invocable as a REST API, runs as Asynchronous job. Hence, the analysis can be done on millions of documents. 

Please refer the build, installation and run instructions. Refer examples folder for configurations



