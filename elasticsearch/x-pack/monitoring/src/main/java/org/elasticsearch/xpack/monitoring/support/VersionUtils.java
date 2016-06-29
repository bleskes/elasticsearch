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

package org.elasticsearch.xpack.monitoring.support;

import org.elasticsearch.Version;
import org.elasticsearch.common.Strings;

import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public final class VersionUtils {

    public static final String VERSION_NUMBER_FIELD = "number";

    private VersionUtils() {
    }

    public static Version parseVersion(byte[] text) {
        return parseVersion(VERSION_NUMBER_FIELD, new String(text, Charset.forName("UTF-8")));
    }

    /**
     * Extract &amp; parse the version contained in the given template
     */
    public static Version parseVersion(String prefix, byte[] text) {
        return parseVersion(prefix, new String(text, Charset.forName("UTF-8")));
    }

    public static Version parseVersion(String prefix, String text) {
        Pattern pattern = Pattern.compile(prefix + "\"\\s*:\\s*\"?([0-9a-zA-Z\\.\\-]+)\"?");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String parsedVersion = matcher.group(1);
            if (Strings.hasText(parsedVersion)) {
                return Version.fromString(parsedVersion);
            }
        }
        return null;
    }
}
