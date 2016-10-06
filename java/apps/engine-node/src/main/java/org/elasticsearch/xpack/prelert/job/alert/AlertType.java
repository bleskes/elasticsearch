
package org.elasticsearch.xpack.prelert.job.alert;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;


public enum AlertType {
    BUCKET {
        @Override
        @JsonValue
        public String toString() {
            return new String("bucket");
        }
    },

    INFLUENCER {
        @Override
        @JsonValue
        public String toString() {
            return new String("influencer");
        }
    },

    BUCKETINFLUENCER {
        @Override
        @JsonValue
        public String toString() {
            return new String("bucketinfluencer");
        }
    };

    @JsonCreator
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