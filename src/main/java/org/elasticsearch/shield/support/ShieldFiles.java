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

package org.elasticsearch.shield.support;

import org.elasticsearch.common.base.Charsets;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.*;

public class ShieldFiles {

    private ShieldFiles() {}

    /**
     * This writer opens a temporary file instead of the specified path and
     * tries to move the create tempfile to specified path on close. If possible
     * this move is tried to be atomic, but it will fall back to just replace the
     * existing file if the atomic move fails.
     *
     * If the destination path exists, it is overwritten
     *
     * @param path The path of the destination file
     * @return
     * @throws IOException
     */
    public static final Writer openAtomicMoveWriter(final Path path) throws IOException {
        final Path tempFile = Files.createTempFile(path.getParent(), path.getFileName().toString(), "tmp");
        final Writer writer = Files.newBufferedWriter(tempFile, Charsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        return new Writer() {
            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
                writer.write(cbuf, off, len);
            }

            @Override
            public void flush() throws IOException {
                writer.flush();
            }

            @Override
            public void close() throws IOException {
                writer.close();
                try {
                    Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        };
    }
}
