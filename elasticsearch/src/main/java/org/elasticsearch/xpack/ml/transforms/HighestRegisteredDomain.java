/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.ml.transforms;

import java.util.List;

import org.apache.logging.log4j.Logger;


/**
 * Split a hostname into Highest Registered Domain and sub domain.
 * TODO Reimplement porting the code from C++
 */
public class HighestRegisteredDomain extends Transform {
    /**
     * Immutable class for the domain split results
     */
    public static class DomainSplit {
        private String subDomain;
        private String highestRegisteredDomain;

        private DomainSplit(String subDomain, String highestRegisteredDomain) {
            this.subDomain = subDomain;
            this.highestRegisteredDomain = highestRegisteredDomain;
        }

        public String getSubDomain() {
            return subDomain;
        }

        public String getHighestRegisteredDomain() {
            return highestRegisteredDomain;
        }
    }

    public HighestRegisteredDomain(List<TransformIndex> readIndexes, List<TransformIndex> writeIndexes, Logger logger) {
        super(readIndexes, writeIndexes, logger);
    }

    @Override
    public TransformResult transform(String[][] readWriteArea) {
        return TransformResult.FAIL;
    }
}
