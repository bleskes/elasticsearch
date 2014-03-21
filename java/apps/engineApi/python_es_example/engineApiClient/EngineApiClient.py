#! /usr/bin/python
############################################################
#                                                          #
# Contents of file Copyright (c) Prelert Ltd 2006-2014     #
#                                                          #
#----------------------------------------------------------#
#----------------------------------------------------------#
# WARNING:                                                 #
# THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               #
# SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     #
# PARENT OR SUBSIDIARY COMPANIES.                          #
# PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         #
#                                                          #
# This source code is confidential and any person who      #
# receives a copy of it, or believes that they are viewing #
# it without permission is asked to notify Prelert Ltd     #
# on +44 (0)20 7953 7243 or email to legal@prelert.com.    #
# All intellectual property rights in this source code     #
# are owned by Prelert Ltd.  No part of this source code   #
# may be reproduced, adapted or transmitted in any form or #
# by any means, electronic, mechanical, photocopying,      #
# recording or otherwise.                                  #
#                                                          #
#----------------------------------------------------------#
#                                                          #
#                                                          #
############################################################

import httplib
import json
import logging

class EngineApiClient:
    '''
    A simple HTTP client to the Prelert Engine REST API
    '''

    def __init__(self, host, base_url, port=8080):
        '''
        Create a HTTP connection to host:port
        host is the host machine
        base_url is the API URl this should contain the version number e.g. /engine/beta
        '''
        self.host = host

        # ensure it starts with "/"
        if not base_url.startswith("/"):
            base_url = "/" + base_url

        logging.info("Connecting to Engine REST API at {0}:{1}{2}".format(host, port, base_url))
        self.base_url = base_url
        self.connection = httplib.HTTPConnection(host, port)


    def getJobs(self):
        '''
        Return all the jobs in the system.
        '''
        self.connection.request("GET", self.base_url + "/jobs")
        response = self.connection.getresponse();
        logging.debug("Get jobs response = " + str(response.status) + " " + response.reason)
        if response.status != 200:
            logging.error("Error cannot get job = " + response.read())

        jobs = json.load(response)

        return jobs


    def createJob(self, payload):
        '''
        Create a new job. payload is the Json format job creation string.
        Returns the newly created job id.
        '''
        headers = {'Content-Type':'application/json'}

        self.connection.request("POST", self.base_url + "/jobs", payload, headers)
        response = self.connection.getresponse();
        
        logging.debug("Create job response = " + str(response.status) + " " + response.reason)
        
        if response.status != 201:
            logging.error("Error creating job. Result = " + response.read())        
            return ""

        data = json.load(response)
        return data['id']     


    def upload(self, job_id, data):
        '''
        Upload the data to the API data endpoint, data can be a
        file-like object.
        Returns the HTTP status code
        '''
        logging.debug('Uploading data to job ' + job_id + '...')

        headers = {'Content-Type':'application/json'}
        url = self.base_url + "/data/" + job_id

        self.connection.request("POST", url, data, headers)        

        response = self.connection.getresponse();
        logging.debug("Upload file response = " + str(response.status) + " " + response.reason)
        if response.status != 202:
            logging.error("Upload response data = " + response.read())

        response.read() # read all of the response before another request can be made        
        return response.status
        

    def close(self, job_id):
        '''
        Close the job once data has been streamed
        Returns the HTTP status code
        '''
        logging.debug('Closing job ' + job_id)

        url = self.base_url + "/data/" + job_id + "/close"
        self.connection.request("POST", url)
        response = self.connection.getresponse()
        logging.debug("Close response = " + str(response.status) + " " + response.reason)
        if response.status != 202:
            logging.error("Close response data = " + response.read())

        response.read() # read all of the response before another request can be made
        return response.status


    def getResults(self, job_id, include_records=False):
        '''
        Return all the job's bucket results.
        The function makes multiple queries to the API to get all the 
        result buckets through the paging system
        '''

        skip = 0        
        take = 100
        expand = ''
        if include_records:
            expand = 'expand=true'

        
        headers = {'Content-Type':'application/json'}
        url = self.base_url + "/results/{0}?skip={1}&take={2}&{3}".format(job_id, skip, take, expand)
        self.connection.request("GET", url)
        response = self.connection.getresponse();
        logging.debug("Get results response = " + str(response.status) + " " + response.reason)

        result = json.load(response)
        buckets = result['documents']

        # is there another page of results
        while result['nextPage']:
            skip += take
            url = self.base_url + "/results/{0}?skip={1}&take={2}&{3}".format(job_id, skip, take, expand)
            self.connection.request("GET", url)
            response = self.connection.getresponse();
            logging.debug("Get results response = " + str(response.status) + " " + response.reason)
            result = json.load(response)
            buckets.extend(result['documents'])

        return buckets

    def getBucket(self, job_id, bucket_id, include_records=True):
        '''
        Return the individual bucket result and optionally include the 
        anomaly records        
        '''

        logging.debug("Get bucket " + bucket_id)

        expand = ''
        if include_records:
            expand = '?expand=true'

        headers = {'Content-Type':'application/json'}
        url = self.base_url + "/results/{0}/{1}{2}".format(job_id, bucket_id, expand)
        self.connection.request("GET", url)
        response = self.connection.getresponse();
        
        logging.debug("Get bucket response = " + str(response.status) + " " + response.reason)
        if response.status != 200:
            logging.error("Error reading bucket = " + response.read())
            return {}

        result = json.load(response)
        bucket = result['document']

        return bucket        


