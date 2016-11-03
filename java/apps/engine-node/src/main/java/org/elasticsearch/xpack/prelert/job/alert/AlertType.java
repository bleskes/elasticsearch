/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
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
package org.elasticsearch.xpack.prelert.job.alert;


public enum AlertType {
    BUCKET {
        @Override
        public String toString() {
            return new String("bucket");
        }
    },

    INFLUENCER {
        @Override
        public String toString() {
            return new String("influencer");
        }
    },

    BUCKETINFLUENCER {
        @Override
        public String toString() {
            return new String("bucketinfluencer");
        }
    };

    public static AlertType fromString(String str) {
        for (AlertType at : AlertType.values()) {
            if (at.toString().equals(str)) {
                return at;
            }
        }

        throw new IllegalArgumentException("The string '" + str +
                "' cannot be converted to an AlertType");
    }
}