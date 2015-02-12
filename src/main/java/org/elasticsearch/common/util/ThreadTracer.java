/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.common.util;

import com.carrotsearch.hppc.ObjectLongMap;
import com.carrotsearch.hppc.ObjectLongOpenHashMap;
import com.carrotsearch.hppc.cursors.ObjectLongCursor;
import org.elasticsearch.common.logging.support.LoggerMessageFormat;
import org.elasticsearch.common.unit.TimeValue;

import java.util.LinkedList;

public class ThreadTracer {

    final static ThreadLocal<Analysis> threadAnalysis = new ThreadLocal<Analysis>();


    static public void startAnalysis(long slowOpThresholdInMills) {
        assert threadAnalysis.get() == null : "can't double start thread analysis";
        threadAnalysis.set(new Analysis(slowOpThresholdInMills));
    }

    static public Analysis stopAnalysis() {
        Analysis current = threadAnalysis.get();
        threadAnalysis.set(null);
        current.stop();
        return current;
    }

    static public void onOpStart() {
        Analysis current = threadAnalysis.get();
        if (current == null) {
            return;
        }
        current.onOpStart();
    }

    static public void onOpEnd(String category, String desc, Object... params) {
        Analysis current = threadAnalysis.get();
        if (current == null) {
            return;
        }
        current.onOpEnd(category, desc, params);
    }


    public static class Analysis {

        final long slowOpThresholdInMills;
        final long analysisStartTime = System.currentTimeMillis();

        long opCount = 0;
        long totalOpTime = 0;
        long totalAnalysisTime = 0;

        ObjectLongMap<String> opCountPerCategory = new ObjectLongOpenHashMap<>();
        ObjectLongMap<String> totalOpTimePerCategory = new ObjectLongOpenHashMap<>();

        long lastOpStart = -1;

        LinkedList<Operation> slowOperations = new LinkedList<>();

        public Analysis(long slowOpThresholdInMills) {
            this.slowOpThresholdInMills = slowOpThresholdInMills;
        }

        public void onOpStart() {
            assert lastOpStart < 0 : "you can't double start. must end first";
            assert totalAnalysisTime == 0 : "analysis is done. too bad.";
            lastOpStart = System.currentTimeMillis();
        }

        public void onOpEnd(String category, String desc, Object... params) {
            assert lastOpStart > 0 : "operation end called with no operation start";
            long totalTime = System.currentTimeMillis() - lastOpStart;
            opCount++;
            totalOpTime += totalTime;

            opCountPerCategory.addTo(category, 1);
            totalOpTimePerCategory.addTo(category, totalTime);

            if (totalTime > slowOpThresholdInMills) {
                slowOperations.add(new Operation(category, LoggerMessageFormat.format(desc, params), totalTime));
            }
            lastOpStart = -1;
        }

        public void stop() {
            totalAnalysisTime = System.currentTimeMillis() - analysisStartTime;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("took [").append(TimeValue.timeValueMillis(totalAnalysisTime))
                    .append("], with [").append(opCount).append("] ops. Total ops time [")
                    .append(TimeValue.timeValueMillis(totalOpTime)).append("]");
            if (opCountPerCategory.size() > 0) {
                sb.append("\n--> break down per category:");
                for (ObjectLongCursor<String> stat : opCountPerCategory) {
                    sb.append("\n  - ").append(stat.key).append(": [").append(opCountPerCategory.get(stat.key)).append("] ops. total ops time [")
                            .append(TimeValue.timeValueMillis(totalOpTimePerCategory.get(stat.key))).append("]");
                }
            }
            if (slowOperations.size() > 0) {
                sb.append("\n--> slow ops (time >[").append(TimeValue.timeValueMillis(slowOpThresholdInMills)).append("]):");
                for (Operation op : slowOperations) {
                    sb.append("\n  - ").append(op.category).append(": ").append(op.description)
                            .append(" took [").append(TimeValue.timeValueMillis(op.totalTimeInMills)).append("]");
                }
            }

            return sb.toString();
        }
    }

    public static class Operation {
        final public String category;
        final public String description;
        final public long totalTimeInMills;

        public Operation(String category, String description, long totalTimeInMills) {
            this.category = category;
            this.description = description;
            this.totalTimeInMills = totalTimeInMills;
        }
    }


}
