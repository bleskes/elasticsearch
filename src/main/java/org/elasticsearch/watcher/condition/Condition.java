/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated. All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
 */

package org.elasticsearch.watcher.condition;

import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.xcontent.ToXContent;

/**
 *
 */
public interface Condition extends ToXContent {

    String type();

    abstract class Result implements ToXContent {

        private final String type;
        protected final boolean met;

        public Result(String type, boolean met) {
            this.type = type;
            this.met = met;
        }

        public String type() {
            return type;
        }

        public boolean met() { return met; }

    }

    interface Builder<C extends Condition> {

        C build();
    }

    interface Field {
        ParseField MET = new ParseField("met");
    }
}
