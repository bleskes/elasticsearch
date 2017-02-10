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

import org.elasticsearch.test.ESTestCase;

import java.nio.file.Files;
import java.nio.file.Path;

public class XPackExtensionsServiceTests extends ESTestCase {
    public void testExistingPluginMissingDescriptor() throws Exception {
        Path extensionsDir = createTempDir();
        Files.createDirectory(extensionsDir.resolve("extension-missing-descriptor"));
        IllegalStateException e = expectThrows(IllegalStateException.class, () -> {
            XPackExtensionsService.getExtensionBundles(extensionsDir);
        });
        assertTrue(e.getMessage(),
                e.getMessage().contains("Could not load extension descriptor for existing extension"));
    }
}
