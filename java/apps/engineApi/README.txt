Prelert Engine API
==================

The Prelert Engine API is a RESTful anomaly detector built to automatically
identify anomalies in large data volumes.

The RESTful interface can create analytic jobs, upload data to those jobs, and
persist the results in a queryable data store. The API is modeled around the
concept of jobs where each job encapsulates the analytics task, the
configuration for that task and the results of the analytics. Jobs have an
immutable configuration that once created cannot be modified. Each job has a
unique jobId that must be used in all job related actions including uploading
data and querying results.

Full documentation is available on the Prelert website:

http://www.prelert.com/docs/engine_api/2.1/


Getting Started
===============

This example code below assumes that the Engine API is installed locally on the
default TCP port (8080). If you are working with a remote instance, please
substitute "localhost:8080" with your remote "ipaddress:port" details.

First check the installation is running:

curl 'http://localhost:8080/engine/v2/'

This will return the version number of the Engine API. Don't worry if the
version or build number is not exactly the same as the example below. If the
version number is lower we recommend upgrading to a newer version.

<!DOCTYPE html>
<html>
<head><title>Prelert Engine</title></head>
<body>
<h1>Prelert Engine API 2.0.3</h1>
<h2>Analytics Version:</h2>
<p>Model State Version 25<br>prelert_autodetect_api (64 bit): Version 6.1.4 (Build ce9cd52f473cc7) Copyright (c) Prelert Ltd 2006-2016<br></p>
<div><table><tr><td>Hostname</td><td>hostname.local</td></tr><tr><td>OS Name</td><td>Linux</td></tr><tr><td>OS Version</td><td>3.10.0</td></tr><tr><td>Total Memory Size MB</td><td>16384</td></tr><tr><td>Total Disk MB</td><td>475672</td></tr><tr><td>Available Disk MB</td><td>117076</td></tr></table></div></body>
</html>

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
http://s3.amazonaws.com/prelert_demo/farequote.csv. Time series data are
preferred to be ordered by date. (In case data cannot be sent in time order, see
http://www.prelert.com/docs/engine_api/2.1/concepts/outofsequence.html.) The raw
CSV data looks like this:

time,airline,responsetime,sourcetype
2014-06-23 00:00:00Z,AAL,132.2046,farequote
2014-06-23 00:00:00Z,JZA,990.4628,farequote
2014-06-23 00:00:00Z,JBU,877.5927,farequote
2014-06-23 00:00:00Z,KLM,1355.4812,farequote
2014-06-23 00:00:00Z,NKS,9991.3981,farequote
...

1. Create New Job
-----------------

This creates an analysis jobs for the example data file. This will baseline
responsetime for all airlines and report if any responsetime value deviates
significantly from its baseline.

Creating a new job requires both a declaration of how the data is formatted
(dataDescription), and how the data is expected to be analyzed (analysisConfig).

curl -X POST -H 'Content-Type: application/json' 'http://localhost:8080/engine/v2/jobs' -d '{
    "id":"farequote",
    "analysisConfig" : {
        "bucketSpan":3600,
        "detectors" :[{"function":"metric","fieldName":"responsetime","byFieldName":"airline"}],
        "influencers" :["airline"]
    },
    "dataDescription" : {
        "fieldDelimiter":",",
        "timeField":"time",
        "timeFormat":"yyyy-MM-dd HH:mm:ssX"
    }
}'

In this example we are creating a new job with the ID 'farequote' and specifying
that we want the analysis to be executed on the 'responsetime' field. This field
contains a numeric value, so we specify the metric function, which expands to all
of min, mean, and max. (Had we wanted to look at event rate or rare fields
we'd have used one of the other available functions.) By declaring byFieldName as
'airline', the analysis will be performed across all airlines, instead of a
unique analysis done for each of the 19 airlines.

bucketSpan defines that the analysis should be performed across hourly (3600
second) windows.

The dataDescription section gives clues as to how the data is formatted, what
character delimits the fields, and what is the format of the timestamp.

The cURL command will return the job ID specified in the configuration

{"id":"farequote"}

2. Check Job Status
-------------------

Now the the analysis job is created, you can check out the details of the job:

curl 'http://localhost:8080/engine/v2/jobs'

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
    "location" : "http://localhost:8080/engine/v2/jobs/farequote",
    "id" : "farequote",
    "analysisConfig" : {
      "latency" : 0,
      "bucketSpan" : 3600,
      "detectors" : [ {
        "fieldName" : "responsetime",
        "function" : "metric",
        "byFieldName" : "airline"
      } ],
      "influencers" : [ "airline" ]
    },
    "dataDescription" : {
      "format" : "DELIMITED",
      "fieldDelimiter" : ",",
      "timeField" : "time",
      "quoteCharacter" : "\"",
      "timeFormat" : "yyyy-MM-dd HH:mm:ssX"
    },
    "status" : "CLOSED",
    "timeout" : 600,
    "dataEndpoint" : "http://localhost:8080/engine/v2/data/farequote",
    "resultsEndpoint" : "http://localhost:8080/engine/v2/results/farequote",
    "logsEndpoint" : "http://localhost:8080/engine/v2/logs/farequote",
    "counts" : {
      "processedRecordCount" : 0,
      "processedBytes" : 0,
      "invalidDateCount" : 0,
      "missingFieldCount" : 0,
      "outOfOrderTimeStampCount" : 0,
      "processedDataPointCount" : 0
    },
    "createTime" : "2014-08-22T12:49:52.287+0000"
  } ]
}

For detailed explanation of the output, please refer to the jobResource object
in the reference documentation. For now, note that the key piece of information
is the jobId, which uniquely identifies this job and will be used in the
remainder of this tutorial.

3. Upload Data
--------------

Now we can send the CSV data to the /data endpoint to be processed by the
engine. Using cURL, we will use the -T option to upload the file. You will need
to edit the URL to contain the jobId and specify the path to the
farequote.csv file:

curl -X POST -T farequote.csv 'http://localhost:8080/engine/v2/data/farequote'

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

curl -X POST 'http://localhost:8080/engine/v2/data/farequote/close'

Note: in the case of the farequote.csv example data you'll have enough
results to see the anomaly by the time the upload has completed even if you
don't close the job.

5. View Results
---------------

We can request the /results endpoint for our jobId to see what kind of results
are available:

curl 'http://localhost:8080/engine/v2/results/farequote/buckets?skip=0&take=100'

This returns a summary of the anomalousness of the data, for each time interval.
If not set 'skip' and 'take' default to 0 and 100 meaning the first 100 results are returned

{
  "hitCount" : 119,
  "skip" : 0,
  "take" : 100,
  "nextPage" : "http://localhost:8080/engine/v2/results/farequote/buckets?skip=100&take=100&expand=false",
  "previousPage" : null,
  "documents" : [ {
    "timestamp" : "2014-06-23T00:00:00.000+0000",
    "bucketSpan" : 3600,
    "rawAnomalyScore" : 0.0,
    "recordCount" : 0,
    "eventCount" : 649,
    "records" : [ ],
    "anomalyScore" : 0.0,
    "maxNormalizedProbability" : 0.0
  }, {
    "timestamp" : "2014-06-23T01:00:00.000+0000",
    "bucketSpan" : 3600,
    "rawAnomalyScore" : 0.0,
    "recordCount" : 0,
    "eventCount" : 627,
    "records" : [ ],
    "anomalyScore" : 0.0,
    "maxNormalizedProbability" : 0.0
  }, {
  ...
  }
}

The Engine API Dashboard gives a visual view of the results. This can be found
here: http://localhost:5601/app/prelert

In practice, most implementations will process the results programatically.
For the purpose of this tutorial, we will continue using the cURL command line
and jump straight to the bucket with the maximum anomalyScore. This has the
following timestamp: 1403712000.

We can request the details of just this one bucket interval as follows:

curl 'http://localhost:8080/engine/v2/results/farequote/buckets/1403712000?expand=true'

{
  "documentId" : "1403712000",
  "exists" : true,
  "type" : "bucket",
  "document" : {
    "timestamp" : "2014-06-25T16:00:00.000+0000",
    "bucketSpan" : 3600,
    "rawAnomalyScore" : 26.0817,
    "recordCount" : 1,
    "eventCount" : 909,
    "records" : [ {
      "timestamp" : "2014-06-25T16:00:00.000+0000",
      "fieldName" : "responsetime",
      "function" : "mean",
      "byFieldName" : "airline",
      "probability" : 2.36652E-89,
      "anomalyScore" : 94.35376,
      "normalizedProbability" : 100.0,
      "byFieldValue" : "AAL",
      "typical" : 99.8279,
      "actual" : 242.75
    } ],
    "anomalyScore" : 94.35376,
    "maxNormalizedProbability" : 100.0
  }
}

This shows that between 2014-06-25T16:00:00-0000 and 2014-06-25T17:00:00-0000
(the bucket start time and bucketSpan) the responsetime for airline AAL increased
from a normal mean value of 99.8279 to 242.75. The probability of seeing 242.75
is 2.36652E-89 (which is very unlikely).

This increased value is highly unexpected based upon the past behavior of this
metric and is thus an outlier.

6. Delete Job
-------------

Finally, the job can be deleted which shuts down all resources associated with
the job, and deletes the results:

curl -X DELETE 'http://localhost:8080/engine/v2/jobs/farequote'

7. Using JSON data
------------------

The same data can be processed in JSON format. Download the example data from
http://s3.amazonaws.com/prelert_demo/farequote.json. The format of the file is
as follows:

{"airline": "AAL", "responsetime": "132.2046", "sourcetype": "farequote", "time": "1447804800"}
{"airline": "JZA", "responsetime": "990.4628", "sourcetype": "farequote", "time": "1447804800"}
{"airline": "JBU", "responsetime": "877.5927", "sourcetype": "farequote", "time": "1447804800"}
...

The same steps as above can be followed, except that the dataDescription would
need to be altered during the job creation:

curl -X POST -H 'Content-Type: application/json' 'http://localhost:8080/engine/v2/jobs' -d '{
    "id" : "farequote-json",
    "analysisConfig" : {
        "bucketSpan":3600,
        "detectors" :[{"fieldName":"responsetime","byFieldName":"airline"}],
        "influencers" :["airline"]
    },
    "dataDescription" : {
        "format":"json",
        "timeField":"time"
    }
}'

And the upload data step would need to point to the JSON file:

curl -X POST -T farequote.json 'http://localhost:8080/engine/v2/data/farequote-json'

After closing the job review the bucket results as before

curl -X POST 'http://localhost:8080/engine/v2/data/farequote-json/close'
curl 'http://localhost:8080/engine/v2/results/farequote-json/buckets'

Further information
===================

To view the full documentation, please visit:

http://www.prelert.com/docs/engine_api/2.1/

Code examples are also available from:

http://github.com/prelert/

