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

package org.elasticsearch.shield.test;

import com.google.common.base.Charsets;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.io.FileSystemUtils;
import org.elasticsearch.common.io.Streams;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class ShieldTestUtils {

    public static File createFolder(File parent, String name) {
        File createdFolder = new File(parent, name);
        //the directory might exist e.g. if the global cluster gets restarted, then we recreate the directory as well
        if (createdFolder.exists()) {
            if (!FileSystemUtils.deleteRecursively(createdFolder)) {
                throw new RuntimeException("could not delete existing temporary folder: " + createdFolder.getAbsolutePath());
            }
        }
        if (!createdFolder.mkdir()) {
            throw new RuntimeException("could not create temporary folder: " + createdFolder.getAbsolutePath());
        }
        return createdFolder;
    }

    public static String writeFile(File folder, String name, byte[] content) {
        Path file = folder.toPath().resolve(name);
        try {
            Streams.copy(content, file.toFile());
        } catch (IOException e) {
            throw new ElasticsearchException("error writing file in test", e);
        }
        return file.toFile().getAbsolutePath();
    }

    public static String writeFile(File folder, String name, String content) {
        return writeFile(folder, name, content.getBytes(Charsets.UTF_8));
    }
}
