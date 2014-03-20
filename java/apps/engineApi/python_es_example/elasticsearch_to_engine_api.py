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
import csv
import sys
import json
import logging

from elasticsearch import Elasticsearch
from elasticsearch.client import IndicesClient

from engineApiClient import EngineApiClient

'''
    This script searches Elasticsearch for all documents of type 'farequote'
    that should contain the fields '_time', 'airline' and 'responsetime'. 
    The returned documents are then iteratively uploaded to the Prelert Engine
    REST API.

    The Elasticsearch host, port and name of the index containing the data can 
    all be specified with command line arguments.
'''

# Elasticsearch default settings
ES_HOST = 'localhost'
ES_PORT = 9200
ES_INDEX = "prelerttestjob" 
DOC_TYPE = "farequote"

# Prelert Engine API connection prarams
API_HOST = 'localhost'
API_PORT = 8080
ABI_BASE_URL = 'engine/v0.3'

# The number of documents to request from Elasticsearch in each query
NUM_DOCS_TAKE = 10000

# The Job creation config
# Bucketspan is set to 1 hour the data format is Json with
# the timestamp in the '_time' field. The  analysis is 
# max(responsetime) by airline
JOB_CONFIGURATION = '{"analysisConfig" : { "bucketSpan":3600,' \
            '"detectors":[{"function":"max","fieldName":"responsetime","byFieldName":"airline"}] },' \
            '"dataDescription":{"format":"json","timeField":"_time"} }'


def setupLogging():
    '''
        Log to console
    '''    
    logging.basicConfig(level=logging.INFO,format='%(asctime)s %(levelname)s %(message)s')

def parseArguments():
    parser = argparse.ArgumentParser()
    parser.add_argument("--index", help="The Elasticsearch index to read the \
        data records from, defaults to '" + ES_INDEX + "'", default=ES_INDEX)
    parser.add_argument("--es_host", help="The host machine Elasticsearch is \
        running on, defaults to '" + ES_HOST + "'", default=ES_HOST)
    parser.add_argument("--es_port", help="The Elasticsearch port, defaults to " 
        + str(ES_PORT), default=ES_PORT)
    parser.add_argument("--api_host", help="The Prelert Engine API host, defaults to "
        + API_HOST, default=API_HOST)    
    parser.add_argument("--api_port", help="The Prelert Engine API port, defaults to " 
        + str(API_PORT), default=API_PORT)

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
    ES_HOST = args.es_host
    ES_PORT = args.es_port
    ES_INDEX = args.index

    API_HOST = args.api_host
    API_PORT = args.api_port

    # The REST API client
    engine_client = EngineApiClient.EngineApiClient(API_HOST, ABI_BASE_URL, API_PORT)
    job_id = engine_client.createJob(JOB_CONFIGURATION)
    logging.info("Created job with id " + job_id)

    # The ElasticSearch client
    es_client = Elasticsearch(ES_HOST + ":" + str(ES_PORT))


    # Search for all the documents sorted in ascending time order
    # 100 documents at a time
    search_body = '{"query" : {"match_all" : {}}, "sort":[{"_time" : "asc"}] }'
    fields = ['_time', 'airline', 'responsetime']
    first_doc_index = 0

    # Query the documents from ElasticSearch and write to the Engine
    hits = es_client.search(index=ES_INDEX, doc_type=DOC_TYPE, body=search_body, 
        _source=True, _source_include=fields, from_=first_doc_index, size=NUM_DOCS_TAKE)


    while len(hits['hits']['hits']) > 0:    
        content = json.dumps(elasticSearchDocsToDicts(hits['hits']['hits']))
        engine_client.upload(job_id, content)

        first_doc_index += NUM_DOCS_TAKE
        hits = es_client.search(index=ES_INDEX, doc_type=DOC_TYPE, body=search_body, 
            _source=True, _source_include=fields, from_=first_doc_index, size=NUM_DOCS_TAKE)



if __name__ == "__main__":
    main()    

