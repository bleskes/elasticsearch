README
=======
Use this Java command to upload a large file to the Prelent Engine API.
The program takes 2 arguments the data endpoint of the API and the data file,
optionally the --compressed and --close flags can be set if the data is gzip
compressed and the job should be closed after the data is uploaded.

Example Usage
--------------
java -cp '.:./*' com.prelert.rs.client.StreamFile 'http://localhost:8080/engine/v2/data/<job_id>' /path/to/data.csv.gz --compressed --close
or
java -cp '.:./*' com.prelert.rs.client.StreamFile --help


The required jar files are:
prelert-engine-api-common.jar - The Prelert Engine API common core
prelert-engine-api-client.jar - The Prelert Engine API client
guava-19.0.jar
jackson-annotations-2.6.5.jar
jackson-core-2.6.5.jar
jackson-databind-2.6.5.jar
jetty-client-9.3.7.v20160115.jar
jetty-http-9.3.7.v20160115.jar
jetty-io-9.3.7.v20160115.jar
jetty-util-9.3.7.v20160115.jar
log4j-1.2.17.jar

