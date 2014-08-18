Prelert Engine API
==================

The Prelert Engine API is a RESTful anomaly detector built to automatically
identify anomalies large data volumes.

The RESTful interface can create analytic jobs, upload data to those jobs, and
persist the results in a queryable data store. The API is modeled around the
concept of jobs where each job encapsulates the analytics task, the
configuration for that task and the results of the analytics. Jobs have an
immutable configuration that once created cannot be modified. Each job has a
unique jobId that must be used in all job related actions including uploading
data and querying results.

Full documentation is available on the Prelert website:

http://www.prelert.com/docs/engine_api/latest/


Getting Started
===============

This example code below assumes that the Engine API is installed locally on the
default TCP port (8080). If you are working with a remote instance, please
substitute "localhost:8080" with your remote "ipaddress:port" details.

First check the installation is running:

curl 'http://localhost:8080/engine/v1'

This will return the version number of the Engine API. Don't worry if the
version or build number is not exactly the same as the example below. If the
version number is lower we recommend upgrading to a newer version.

<!DOCTYPE html>
<html>
<head><title>Prelert Engine</title></head>
<body>
<h1>Prelert Engine REST API</h1>
<h2>Analytics Version:</h2>
<p>prelert_autodetect_api (64 bit): Version 4.2.6 (Build 20140321084208) Copyright (c) Prelert Ltd 2006-2014</p>
</body>

If it's not already running then start it using:

$PRELERT_HOME/bin/prelert_startup.sh

where $PRELERT_HOME is the directory you chose during the installation.


Your first tutorial
===================

Let's now try to analyze an example time series dataset. This data has been
taken from a fictional flight comparison website where users can request
real-time quotes from multiple airlines. The website makes programatic calls to
each airline to get their latest fare information. It is important that this
data request is quick, as slow responding airlines will negatively impact user
experience as a whole.

Here we will investigate response times to by airline.

In this exercise we will be using the cURL command-line utility (from
http://curl.haxx.se) which allows the easy transfer of data using a URL syntax
over HTTP.

Before we start, please download the example CSV file from
http://s3.amazonaws.com/prelert_demo/farequote.csv.
Time series data must be ordered by date. The raw csv data looks like this:

time,airline,responsetime,sourcetype
2013-01-28 00:00:00Z,AAL,132.2046,farequote
2013-01-28 00:00:00Z,JZA,990.4628,farequote
2013-01-28 00:00:00Z,JBU,877.5927,farequote
2013-01-28 00:00:00Z,KLM,1355.4812,farequote
2013-01-28 00:00:00Z,NKS,9991.3981,farequote
...

1. Create New Job
-----------------

This creates an analysis jobs for the example data file. This will baseline
responsetime for all airlines and report if any responsetime value deviates
significantly from its baseline.

Creating a new job requires both a declaration of how the data is formatted
(dataDescription), and how the data is expected to be analyzed (analysisConfig).

curl -X POST -H 'Content-Type: application/json' 'http://localhost:8080/engine/v1/jobs' -d '{
    "analysisConfig" : {
        "bucketSpan":3600,
        "detectors" :[{"function":"metric","fieldName":"responsetime","byFieldName":"airline"}]
    },
    "dataDescription" : {
        "fieldDelimiter":",",
        "timeField":"time",
        "timeFormat":"yyyy-MM-dd HH:mm:ssX"
    }
}'

In this example, we are specifying that we want the analysis to be executed on
the 'responsetime' field. This field contains a numeric value, so we specify the
metric function, which expands to all of min, mean, max, and sum. (Had we wanted
to look at event rate or rare fields we'd have used one of the other available
functions.) By declaring byFieldName as 'airline', the analysis will be
performed across all airlines, instead of a unique analysis done for each of the
19 airlines.

bucketSpan defines that the analysis should be performed across hourly (3600
second) windows.

The dataDescription section gives clues as to how the data is formatted, what
character delimits the fields, and what is the format of the timestamp.

This will return a unique job number that will be used to in the remainder of
the tutorial. For example:

{"id":"20140519113920-00001"}

2. Check Job Status
-------------------

Now the the analysis job is created, you can check out the details of the job:

curl 'http://localhost:8080/engine/v1/jobs'

The response will give detailed information about job statuses and their
configuration. If you have multiple jobs running, check createTime for the
latest job. For example:

{
  "hitCount" : 1,
  "skip" : 0,
  "take" : 100,
  "nextPage" : null,
  "previousPage" : null,
  "documents" : [ {
    "analysisLimits" : null,
    "status" : "CLOSED",
    "timeout" : 600,
    "location" : "http://localhost:8080/engine/v1/jobs/20140519113920-00001",
    "dataEndpoint" : "http://localhost:8080/engine/v1/data/20140519113920-00001",
    "resultsEndpoint" : "http://localhost:8080/engine/v1/results/20140519113920-00001",
    "logsEndpoint" : "http://localhost:8080/engine/v1/logs/20140519113920-00001",
    "analysisConfig" : {
      "bucketSpan" : 3600,
      "batchSpan" : null,
      "detectors" : [ {
        "function" : "metric",
        "fieldName" : "responsetime",
        "byFieldName" : "airline"
      } ],
      "period" : null
    },
    "finishedTime" : null,
    "lastDataTime" : null,
    "id" : "20140519113920-00001",
    "dataDescription" : {
      "timeField" : "time",
      "timeFormat" : "yyyy-MM-dd HH:mm:ssX",
      "fieldDelimiter" : ",",
      "quoteCharacter" : "\"",
      "format" : "DELINEATED"
    },
    "createTime" : "2014-05-19T10:39:20.499+0000"
  } ]
}

For detailed explanation of the output, please refer to the jobResource object
in the reference documentation. For now, note that the key piece of information
is the jobId, which uniquely identifies this job and will be used in the
remainder of this tutorial. For example:

{"id":"20140519113920-00001"}

3. Upload Data
--------------

Now we can send the CSV data to the /data endpoint to be processed by the
engine. Using cURL, we will use the -T option to upload the file. You will need
to edit the URL to contain the jobId and specify the path to the
farequote.csv file:

curl -X POST -T farequote.csv 'http://localhost:8080/engine/v1/data/20140519113920-00001'

This will stream the file farequote.csv to the REST API for
analysis. This should take less than a minute on modern commodity hardware.
Once the command prompt returns, the data upload has completed. Next, we can
start looking at the analysis results.

4. Close the Job
----------------

Since we have uploaded a batch of data with a definite end point it's best
practice to close the job before requesting results.  Closing the job tells
the API to flush through any data that's being buffered and store all results.
Once again, you will need to edit the URL to contain the correct jobId:

curl -X POST 'http://localhost:8080/engine/v1/data/20140519113920-00001/close'

Note: in the case of the farequote.csv example data you'll have enough
results to see the anomaly by the time the upload has completed even if you
don't close the job.

5. View Results
---------------

We can request the /results endpoint for our jobId to see what kind of results
are available:

curl 'http://localhost:8080/engine/v1/results/20140519113920-00001?skip=0&take=100'

This returns a summary of the anomalousness of the data, for each time interval.
If not set 'skip' and 'take' default to 0 and 100 meaning the first 100 results are returned

{
  "hitCount" : 118,
  "skip" : 0,
  "take" : 100,
  "nextPage" : "http://localhost:8080/engine/v1/results/20140519113920-00001?skip=100&take=100",
  "previousPage" : null,
  "documents" : [ {
    "recordCount" : 1,
    "timestamp" : "2013-01-28T00:00:00.000Z",
    "id" : "1359331200",
    "rawAnomalyScore" : 0.0
    "anomalyScore" : 0.0
    "unusualScore" : 0.0
  }, {
    "recordCount" : 1,
    "timestamp" : "2013-01-28T01:00:00.000Z",
    "id" : "1359334800",
    "rawAnomalyScore" : 0.0
    "anomalyScore" : 0.0
    "unusualScore" : 0.0
  }, {
  ...
  }
}

The Engine API Dashboard gives a visual view of the results. This can be found
here: http://localhost:8080/dashboard.

In practice, most implementations will process the results programatically.
For the purpose of this tutorial, we will continue using the cURL command line
and jump straight to the bucket with the maximum anomalyScore. This has the
following id: 1359561600.

We can request the details of just this one bucket interval as follows:

curl 'http://localhost:8080/engine/v1/results/20140519113920-00001/1359561600?expand=true'

{
  "documentId" : "1359561600",
  "exists" : true,
  "type" : "bucket",
  "document" : {
    "recordCount" : 2,
    "eventCount" : 277,
    "timestamp" : "2013-01-30T16:00:00.000Z",
    "id" : "1359561600",
    "rawAnomalyScore" : 10.276,
    "anomalyScore" : 94.39974,
    "unusualScore" : 100,
    "records" : [ {
      "byFieldName" : "airline",
      "typical" : 101.651,
      "byFieldValue" : "AAL",
      "actual" : 242.75,
      "probability" : 5.24776E-39,
      "anomalyScore" : 94.39974,
      "unusualScore" : 100,
      "fieldName" : "responsetime",
      "function" : "mean"
    } ]
  }
}

This shows that between 2013-01-30T16:00:00-0000 and 2013-01-30T17:00:00-0000 the
responsetime for airline AAL increased from a normal mean value of 101.651 to 242.75.
The probability of seeing 242.75 is 5.24776E-39 (which is very unlikely).

This increased value is highly unexpected based upon the past behavior of this
metric and is thus an outlier.

6. Delete Job
-------------

Finally, the job can be deleted which shuts down all resources associated with
the job, and deletes the results:

curl -X DELETE 'http://localhost:8080/engine/v1/jobs/20140516091341-00001'

7. Using JSON data
------------------

The same data can be processed in JSON format. Download the example data from
http://s3.amazonaws.com/prelert_demo/farequote.json. The format of the file is
as follows:

{"timestamp": 1350824400, "airline": "DJA", "responsetime": 622, "sourcetype": "farequote"}
{"timestamp": 1350824401, "airline": "JQA", "responsetime": 1742, "sourcetype": "farequote"}
{"timestamp": 1350824402, "airline": "GAL", "responsetime": 5339, "sourcetype": "farequote"}
...

The same steps as above can be followed, except that the dataDescription would
need to be altered during the job creation:

curl -X POST -H 'Content-Type: application/json' 'http://localhost:8080/engine/v1/jobs' -d '{
    "analysisConfig" : {
        "bucketSpan":3600,
        "detectors" :[{"fieldName":"responsetime","byFieldName":"airline"}]
    },
    "dataDescription" : {
        "format":"json",
        "timeField":"timestamp"
    }
}'

And the upload data step would need to point to the JSON file:

curl -X POST -T farequote.json 'http://localhost:8080/engine/v1/data/20140516091341-00001'


Further information
===================

To view the full documentation, please visit:

http://www.prelert.com/docs/engine_api/latest/

Code examples are also available from:

http://github.com/prelert/

