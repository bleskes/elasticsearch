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

import com.google.common.collect.Sets;
import org.elasticsearch.common.base.Charsets;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.junit.Test;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Locale;
import java.util.Set;

import static java.nio.file.attribute.PosixFilePermission.*;
import static org.elasticsearch.shield.support.ShieldFiles.openAtomicMoveWriter;
import static org.hamcrest.Matchers.is;

public class ShieldFilesTests extends ElasticsearchTestCase {

    @Test
    public void testThatOriginalPermissionsAreKept() throws Exception {
        Path path = newTempFile().toPath();

        // no posix file permissions, nothing to test, done here
        boolean supportsPosixPermissions = Files.getFileStore(path).supportsFileAttributeView(PosixFileAttributeView.class);
        assumeTrue("Ignoring because posix file attributes are not supported", supportsPosixPermissions);

        Files.write(path, "foo".getBytes(Charsets.UTF_8));

        Set<PosixFilePermission> perms = Sets.newHashSet(OWNER_READ, OWNER_WRITE);
        if (randomBoolean()) perms.add(OWNER_EXECUTE);
        if (randomBoolean()) perms.add(GROUP_EXECUTE);
        if (randomBoolean()) perms.add(OTHERS_EXECUTE);

        Files.setPosixFilePermissions(path, perms);

        try (PrintWriter writer = new PrintWriter(openAtomicMoveWriter(path))) {
            writer.printf(Locale.ROOT, "This is a test");
        }

        Set<PosixFilePermission> permissionsAfterWrite = Files.getPosixFilePermissions(path);
        assertThat(permissionsAfterWrite, is(perms));
    }

}
