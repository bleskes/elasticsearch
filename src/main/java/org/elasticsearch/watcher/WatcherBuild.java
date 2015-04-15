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

package org.elasticsearch.watcher;

import org.elasticsearch.common.io.FastStringReader;
import org.elasticsearch.common.io.Streams;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.joda.time.format.ISODateTimeFormat;

import java.io.IOException;
import java.util.Properties;

import static org.elasticsearch.common.joda.time.DateTimeZone.UTC;
/**
 */
public class WatcherBuild {

    public static final WatcherBuild CURRENT;

    static {
        String hash = "NA";
        String hashShort = "NA";
        String timestamp = "NA";

        try {
            String properties = Streams.copyToStringFromClasspath("/watcher-build.properties");
            Properties props = new Properties();
            props.load(new FastStringReader(properties));
            hash = props.getProperty("hash", hash);
            if (!hash.equals("NA")) {
                hashShort = hash.substring(0, 7);
            }
            String gitTimestampRaw = props.getProperty("timestamp");
            if (gitTimestampRaw != null) {
                timestamp = ISODateTimeFormat.dateTimeNoMillis().withZone(UTC).print(Long.parseLong(gitTimestampRaw));
            }
        } catch (Exception e) {
            // just ignore...
        }

        CURRENT = new WatcherBuild(hash, hashShort, timestamp);
    }

    private final String hash;
    private final String hashShort;
    private final String timestamp;

    WatcherBuild(String hash, String hashShort, String timestamp) {
        this.hash = hash;
        this.hashShort = hashShort;
        this.timestamp = timestamp;
    }

    public String hash() {
        return hash;
    }

    public String hashShort() {
        return hashShort;
    }

    public String timestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WatcherBuild that = (WatcherBuild) o;

        if (!hash.equals(that.hash)) return false;
        if (!hashShort.equals(that.hashShort)) return false;
        if (!timestamp.equals(that.timestamp)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = hash.hashCode();
        result = 31 * result + hashShort.hashCode();
        result = 31 * result + timestamp.hashCode();
        return result;
    }

    public static WatcherBuild readBuild(StreamInput in) throws IOException {
        String hash = in.readString();
        String hashShort = in.readString();
        String timestamp = in.readString();
        return new WatcherBuild(hash, hashShort, timestamp);
    }

    public static void writeBuild(WatcherBuild build, StreamOutput out) throws IOException {
        out.writeString(build.hash());
        out.writeString(build.hashShort());
        out.writeString(build.timestamp());
    }
}
