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

package org.elasticsearch.xpack.extensions;


import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Utility methods for testing extensions */
public class XPackExtensionTestUtil {
    
    /** convenience method to write a plugin properties file */
    public static void writeProperties(Path pluginDir, String... stringProps) throws IOException {
        assert stringProps.length % 2 == 0;
        Files.createDirectories(pluginDir);
        Path propertiesFile = pluginDir.resolve(XPackExtensionInfo.XPACK_EXTENSION_PROPERTIES);
        Properties properties =  new Properties();
        for (int i = 0; i < stringProps.length; i += 2) {
            properties.put(stringProps[i], stringProps[i + 1]);
        }
        try (OutputStream out = Files.newOutputStream(propertiesFile)) {
            properties.store(out, "");
        }
    }
}
