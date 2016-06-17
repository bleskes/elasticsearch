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

package org.elasticsearch.xpack.security.test;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.io.FileSystemUtils;
import org.elasticsearch.common.io.Streams;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class SecurityTestUtils {

    public static Path createFolder(Path parent, String name) {
        Path createdFolder = parent.resolve(name);
        //the directory might exist e.g. if the global cluster gets restarted, then we recreate the directory as well
        if (Files.exists(createdFolder)) {
            try {
                FileSystemUtils.deleteSubDirectories(createdFolder);
            } catch (IOException e) {
                throw new RuntimeException("could not delete existing temporary folder: " + createdFolder.toAbsolutePath(), e);
            }
        } else {
            try {
                Files.createDirectory(createdFolder);
            } catch (IOException e) {
                throw new RuntimeException("could not create temporary folder: " + createdFolder.toAbsolutePath());
            }
        }
        return createdFolder;
    }

    public static String writeFile(Path folder, String name, byte[] content) {
        Path file = folder.resolve(name);
        try (OutputStream os = Files.newOutputStream(file)) {
            Streams.copy(content, os);
        } catch (IOException e) {
            throw new ElasticsearchException("error writing file in test", e);
        }
        return file.toAbsolutePath().toString();
    }

    public static String writeFile(Path folder, String name, String content) {
        return writeFile(folder, name, content.getBytes(StandardCharsets.UTF_8));
    }
}
