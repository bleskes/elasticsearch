Prelert Engine API
==============

Prelert Engine API is a RESTful anomaly detector built to automatically identify
anomalies large data volumes. 

The RESTful interface can create analytic jobs, upload data to those jobs, and persist the results 
in a queryable data store. The API is modeled around the concept of jobs where each job encapsulates 
the analytics task, the configuration for that task and the results of the analytics. Jobs have an 
immutable configuration that once created cannot be modified. Each job has a unique jobId that must 
be used in all job related actions including uploading data and querying results. 

Full documentation is available on the Prelert website:

http://info.prelert.com/engine-api-documentation

http://info.prelert.com/engine-getting-started

Installation
--------------

1. Download: 

    https://prelert.s3.amazonaws.com/builds/engine/prelert_engine_0.3.2_release_linux_64bit.bin?AWSAccessKeyId=AKIAJKPBR6TD5KNSEYKQ&Expires=1396301061&Signature=GZwK44OF3MTX%2FS%2FMZT8dXzlmG1w%3D

2. On the command line run the executable and follow the instructions. 

    ./prelert_engine_0.3.2_release_linux_64bit.bin 

You will need a license key from support@prelert.com

The rest of this tutorial requires you to change to the installation directory. E.g.

    cd /opt/prelert/prelert_home

3. On the command line run:

    bin/startup.sh

This should output to stdout:

Copyright (c) Prelert Ltd 2006-2014
====================================
License installed correctly
------------------------------------
Starting ElasticSearch
Starting the Prelert API

Getting Started
--------------

First check the installation is running:

curl 'http://localhost:8080/engine/v0.3/'

This should return the version number of the Engine:

<!DOCTYPE html>
<html>
<head><title>Prelert Engine</title></head>
<body>
<h1>Prelert Engine REST API</h1>
<h2>Analytics Version:</h2>
<p>prelert_autodetect_api (64 bit): Version 4.2.6 (Build 20140321084208) Copyright (c) Prelert Ltd 2006-2014</p>
</body>

Let's now try to analyse some example time series data:

    examples/farequote_ISO_8601.csv

    time,airline,responsetime,sourcetype
    2013-01-28 00:00:00,AAL,132.2046,farequote
    2013-01-28 00:00:00,JZA,990.4628,farequote
    2013-01-28 00:00:00,JBU,877.5927,farequote
    2013-01-28 00:00:00,KLM,1355.4812,farequote
    2013-01-28 00:00:00,NKS,9991.3981,farequote
    ...

1. Create New Job

This creates an analysis jobs for the example data file. This will baseline responsetime for all
airlines and report if any responsetime value deviates significantly from it's baseline.

curl -X POST -H 'Content-Type: application/json' 'http://localhost:8080/engine/v0.3/jobs' -d '{
    "analysisConfig" : {
        "bucketSpan":3600,
        "detectors" :[{"fieldName":"responsetime","byFieldName":"airline"}]
    },
    "dataDescription" : {
        "fieldDelimiter":",",
        "timeField":"time",
        "timeFormat":"yyyy-MM-dd HH:mm:ss"
    }
}'

This should return a unique job number that will be used to in the remainder of the tutorial. E.g.

{"id":"20140319133023-00005"}

2. Check Job Status

curl 'http://localhost:8080/engine/v0.3/jobs'

This should give detailed information on the job:

{
  "hitCount" : 1,
  "skip" : 0,
  "take" : 100,
  "nextPage" : null,
  "previousPage" : null,
  "documents" : [ {
    "timeout" : 600,
    "status" : "RUNNING",
    "location" : "http://localhost:8080/engine/v0.3/jobs/20140319133023-00005",
    "dataEndpoint" : "http://localhost:8080/engine/v0.3/data/20140319133023-00005",
    "resultsEndpoint" : "http://localhost:8080/engine/v0.3/results/20140319133023-00005",
    "logsEndpoint" : "http://localhost:8080/engine/v0.3/logs/20140319133023-00005",
    "id" : "20140319133023-00005",
    "finishedTime" : null,
    "lastDataTime" : null,
    "dataDescription" : {
      "format" : "DELINEATED",
      "timeField" : "time",
      "timeFormat" : "yyyy-MM-dd HH:mm:ss",
      "fieldDelimiter" : ","
    },
    "analysisConfig" : {
      "bucketSpan" : 3600,
      "batchSpan" : null,
      "partitionField" : null,
      "detectors" : [ {
        "fieldName" : "responsetime",
        "function" : null,
        "byFieldName" : "airline",
        "overFieldName" : null,
        "useNull" : null
      } ],
      "period" : null
    },
    "analysisOptions" : null,
    "createTime" : "2014-03-19T13:30:23.152+0000",
    "processedRecordCount" : 0,
    "persistModel" : true
  } ]
}

3. Upload Data

curl 'http://localhost:8080/engine/v0.3/data/20140319133023-00005' --data-binary @examples/farequote_ISO_8601.csv

This will stream the file examples/farequote_ISO_8601.csv to the REST API for analysis. This should take less 
than a minute on modern commodity hardware.

4. View Results

curl 'http://localhost:8080/engine/v0.3/results/20140319133023-00005'

This returns a summary of the anomalousness of the data at timeintervals:

{
  "hitCount" : 118,
  "skip" : 0,
  "take" : 100,
  "nextPage" : "http://localhost:8080/engine/v0.3/results/20140319133023-00005?skip=100&take=100",
  "previousPage" : null,
  "documents" : [ {
    "recordCount" : 1,
    "timestamp" : "2013-01-28T00:00:00.000Z",
    "id" : "1359331200",
    "anomalyScore" : 0.0
  }, {
    "recordCount" : 1,
    "timestamp" : "2013-01-28T01:00:00.000Z",
    "id" : "1359334800",
    "anomalyScore" : 0.0
  }, {
  ...
  }
}

To view detailed results from a specific bucket:

curl 'http://localhost:8080/engine/v0.3/results/20140319133023-00005/1359561600?expand=true'

{
  "id" : "1359561600",
  "exists" : true,
  "type" : "bucket",
  "document" : {
    "recordCount" : 2,
    "timestamp" : "2013-01-30T16:00:00.000Z",
    "Id" : "1359561600",
    "anomalyScore" : 8.73585,
    "records" : [ {
      "byFieldName" : "airline",
      "typical" : 101.844,
      "byFieldValue" : "AAL",
      "actual" : 242.75,
      "probability" : 4.78331E-34,
      "anomalyScore" : 8.73585,
      "fieldName" : "responsetime",
      "detectorName" : "individual metric/responsetime/airline",
      "function" : "mean"
    }, {
      "isSimpleCount" : true,
      "actual" : 909.0,
      "probability" : 1.0,
      "anomalyScore" : 0.0,
      "detectorName" : "individual count//count",
      "function" : "count"
    } ]
  }
}

This shows that between 2013-01-30T16:00:00.000 and 2013-01-30T17:00:00.000 the responsetime
for airline 'AAL' increased from a normal mean value of 101.844 to 242.75. The 
probability of seeing 242.75 is 4.78331E-34 (which is very unlikely).

5. Delete Job

Finally, the job can be deleted which shuts down all resources associated with the job, and 
deletes the results:

curl -X DELETE 'http://localhost:8080/engine/v0.3/jobs/20140319133023-00005'









