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

package org.elasticsearch.xpack;

import org.elasticsearch.common.SuppressForbidden;
import org.elasticsearch.common.io.PathUtils;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

/**
 * Information about the built version of x-pack that is running.
 */
public class XPackBuild {

    public static final XPackBuild CURRENT;

    static {
        final String shortHash;
        final String date;

        Path path = getElasticsearchCodebase();
        if (path.toString().endsWith(".jar")) {
            try (JarInputStream jar = new JarInputStream(Files.newInputStream(path))) {
                Manifest manifest = jar.getManifest();
                shortHash = manifest.getMainAttributes().getValue("Change");
                date = manifest.getMainAttributes().getValue("Build-Date");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            // not running from a jar (unit tests, IDE)
            shortHash = "Unknown";
            date = "Unknown";
        }

        CURRENT = new XPackBuild(shortHash, date);
    }

    /**
     * Returns path to xpack codebase path
     */
    @SuppressForbidden(reason = "looks up path of xpack.jar directly")
    static Path getElasticsearchCodebase() {
        URL url = XPackBuild.class.getProtectionDomain().getCodeSource().getLocation();
        try {
            return PathUtils.get(url.toURI());
        } catch (URISyntaxException bogus) {
            throw new RuntimeException(bogus);
        }
    }

    private String shortHash;
    private String date;

    XPackBuild(String shortHash, String date) {
        this.shortHash = shortHash;
        this.date = date;
    }

    public String shortHash() {
        return shortHash;
    }

    public String date() {
        return date;
    }
}