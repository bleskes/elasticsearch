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

import argparse
import sys
import json
import logging


from engineApiClient import EngineApiClient

'''
    Print the bucket anomaly score for the given job id.

    The output is csv format of date, bucket id and anomaly score
        Date,BucketId,AnomalyScore
        2014-02-18T12:00:00.000Z,1392724800,0.0
        2014-02-18T13:00:00.000Z,1392728400,0.0
        2014-02-18T14:00:00.000Z,1392732000,0.0

    If a bucket id is specified only the anomaly records for that bucket
    are returned.
'''


# Prelert Engine API connection prarams
API_HOST = 'localhost'
API_PORT = 8080
ABI_BASE_URL = 'engine/v0.3'


def setupLogging():
    '''
        Log to console
    '''    
    logging.basicConfig(level=logging.INFO,format='%(asctime)s %(levelname)s %(message)s')

def parseArguments():
    parser = argparse.ArgumentParser()
    parser.add_argument("--api_host", help="The Prelert Engine API host, defaults to "
        + API_HOST, default=API_HOST)    
    parser.add_argument("--api_port", help="The Prelert Engine API port, defaults to " 
        + str(API_PORT), default=API_PORT)
    parser.add_argument("--bucketid", help="Only get the results for this bucket")

    parser.add_argument("jobid", help="The Id of the Job")

    return parser.parse_args()   


def elasticSearchDocsToDicts(hits):
    '''
        Convert the Elasticsearch hits into an list of dict objects
        In this case we use the '_source' object as the desired fields
        were set in the query.
    '''

    objs = []
    for hit in hits:
        objs.append(hit['_source']) 

    return objs


def main():

    setupLogging()

    args = parseArguments()
    API_HOST = args.api_host
    API_PORT = args.api_port

    # Create the REST API client
    engine_client = EngineApiClient.EngineApiClient(API_HOST, ABI_BASE_URL, API_PORT)

    if args.bucketid != None:
        bucket = engine_client.getBucket(args.jobid, args.bucketid, True)
        print json.dumps(bucket, indent=4, separators=(',', ': '))
    else:
        logging.info("Get result buckets for job " + args.jobid)
        buckets = engine_client.getResults(args.jobid)
        print "Date,BucketId,AnomalyScore"
        for bucket in buckets:
            print "{0},{1},{2}".format(bucket['timestamp'], bucket['Id'], bucket['anomalyScore']) 



if __name__ == "__main__":
    main()    

