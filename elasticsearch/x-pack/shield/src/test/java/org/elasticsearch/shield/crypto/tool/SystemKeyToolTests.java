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

package org.elasticsearch.shield.crypto.tool;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.apache.lucene.util.IOUtils;
import org.elasticsearch.cli.Command;
import org.elasticsearch.cli.CommandTestCase;
import org.elasticsearch.common.io.PathUtilsForTesting;
import org.elasticsearch.shield.crypto.InternalCryptoService;
import org.junit.After;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

public class SystemKeyToolTests extends CommandTestCase {

    private FileSystem jimfs;

    private Path initFileSystem(boolean needsPosix) throws Exception {
        String view = needsPosix ? "posix" : randomFrom("basic", "posix");
        Configuration conf = Configuration.unix().toBuilder().setAttributeViews(view).build();
        jimfs = Jimfs.newFileSystem(conf);
        PathUtilsForTesting.installMock(jimfs);
        return jimfs.getPath("eshome");
    }

    @After
    public void tearDown() throws Exception {
        IOUtils.close(jimfs);
        super.tearDown();
    }

    @Override
    protected Command newCommand() {
        return new SystemKeyTool();
    }

    public void testGenerate() throws Exception {
        final Path homeDir = initFileSystem(true);

        Path path = jimfs.getPath(randomAsciiOfLength(10)).resolve("key");
        Files.createDirectory(path.getParent());

        execute("-Epath.home=" + homeDir, path.toString());
        byte[] bytes = Files.readAllBytes(path);
        // TODO: maybe we should actually check the key is...i dunno...valid?
        assertEquals(InternalCryptoService.KEY_SIZE / 8, bytes.length);

        Set<PosixFilePermission> perms = Files.getPosixFilePermissions(path);
        assertTrue(perms.toString(), perms.contains(PosixFilePermission.OWNER_READ));
        assertTrue(perms.toString(), perms.contains(PosixFilePermission.OWNER_WRITE));
        assertEquals(perms.toString(), 2, perms.size());
    }

    public void testGeneratePathInSettings() throws Exception {
        final Path homeDir = initFileSystem(false);

        Path path = jimfs.getPath(randomAsciiOfLength(10)).resolve("key");
        Files.createDirectories(path.getParent());
        execute("-Epath.home=" + homeDir.toString(), "-Expack.security.system_key.file=" + path.toAbsolutePath().toString());
        byte[] bytes = Files.readAllBytes(path);
        assertEquals(InternalCryptoService.KEY_SIZE / 8, bytes.length);
    }

    public void testGenerateDefaultPath() throws Exception {
        final Path homeDir = initFileSystem(false);
        Path keyPath = homeDir.resolve("config/x-pack/system_key");
        Files.createDirectories(keyPath.getParent());
        execute("-Epath.home=" + homeDir.toString());
        byte[] bytes = Files.readAllBytes(keyPath);
        assertEquals(InternalCryptoService.KEY_SIZE / 8, bytes.length);
    }

    public void testThatSystemKeyMayOnlyBeReadByOwner() throws Exception {
        final Path homeDir = initFileSystem(true);

        Path path = jimfs.getPath(randomAsciiOfLength(10)).resolve("key");
        Files.createDirectories(path.getParent());

        execute("-Epath.home=" + homeDir, path.toString());
        Set<PosixFilePermission> perms = Files.getPosixFilePermissions(path);
        assertTrue(perms.toString(), perms.contains(PosixFilePermission.OWNER_READ));
        assertTrue(perms.toString(), perms.contains(PosixFilePermission.OWNER_WRITE));
        assertEquals(perms.toString(), 2, perms.size());
    }

}
