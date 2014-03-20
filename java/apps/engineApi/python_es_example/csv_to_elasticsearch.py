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

'''
    This script reads the Farequote airline data csv file and writes the 
    records to Elasticsearch. Each time it is run the script tries to create
    a new index and mapping but will fail if the index already exists. Use the 
    --index option to specify a different index name. 
    To delete an index use the Curl:
        curl -X DELETE http://localhost:9200/<index_name>

    This script could be made to run faster if the Elasticsearch bulk 
    update function was used.
'''

# Elasticsearch connection settings
ES_HOST = 'localhost'
ES_PORT = 9200
ES_INDEX = "prelerttestjob"  # The index name must be lower case chars
DOC_TYPE = "farequote"

def parseArguments():
    parser = argparse.ArgumentParser()
    parser.add_argument("--index", help="The Elasticsearch index to store \
        the documents in. The index must consist of lower case characters only, \
        defaults to '" + ES_INDEX + "'", default=ES_INDEX)
    parser.add_argument("--es_host", help="The host machine Elasticsearch is \
        running on, defaults to '" + ES_HOST + "'", default=ES_HOST)
    parser.add_argument("--es_port", help="The Elasticsearch port, defaults to " 
        + str(ES_PORT), default=ES_PORT)
    parser.add_argument("csv_data_file", help="The Farequote csv data file")

    return parser.parse_args()   


def main():

    args = parseArguments()
    ES_HOST = args.es_host
    ES_PORT = args.es_port
    ES_INDEX = args.index

    # The ElasticSearch client
    es_client = Elasticsearch(ES_HOST + ":" + str(ES_PORT))

    # define the ElasticSearch mappings for the document
    mappings = {}
    mappings['mappings'] = {}
    mappings['mappings'][DOC_TYPE] = {}
    mappings['mappings'][DOC_TYPE]['properties'] = {}
    mappings['mappings'][DOC_TYPE]['properties']['airline'] = {"type":"string","index":"not_analyzed"}
    mappings['mappings'][DOC_TYPE]['properties']['responsetime'] = {"type":"double","index":"not_analyzed"}
    mappings['mappings'][DOC_TYPE]['properties']['_time'] = {"type":"long","index":"not_analyzed"}
    mappings['mappings'][DOC_TYPE]['properties']['sourcetype'] = {"type":"string","index":"not_analyzed"}    

    es_client.indices.create(index=ES_INDEX, body=mappings)

    # Read in the csv file and write the Elasticsearch
    f = open(args.csv_data_file, 'rb')
    reader = csv.DictReader(f)
    record_count = 0
    for doc in reader:
        doc_id = str(doc['_time']) + doc['airline']
        es_client.index(index=ES_INDEX, doc_type="farequote", id=doc_id, body=doc)

        record_count += 1
        if record_count % 10000 == 0:
            print "{0} records stored".format(record_count)



if __name__ == "__main__":
    main()    

