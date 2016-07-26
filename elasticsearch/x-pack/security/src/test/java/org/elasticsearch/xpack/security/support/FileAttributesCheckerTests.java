/*
 * ELASTICSEARCH CONFIDENTIAL
 *  __________________
 *
 * [2014] Elasticsearch Incorporated. All Rights Reserved.
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

package org.elasticsearch.xpack.security.support;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.HashSet;
import java.util.Set;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.elasticsearch.cli.MockTerminal;
import org.elasticsearch.test.ESTestCase;

public class FileAttributesCheckerTests extends ESTestCase {

    public void testNonExistentFile() throws Exception {
        Path path = createTempDir().resolve("dne");
        FileAttributesChecker checker = new FileAttributesChecker(path);
        MockTerminal terminal = new MockTerminal();
        checker.check(terminal);
        assertTrue(terminal.getOutput(), terminal.getOutput().isEmpty());
    }

    public void testNoPosix() throws Exception {
        Configuration conf = Configuration.unix().toBuilder().setAttributeViews("basic").build();
        try (FileSystem fs = Jimfs.newFileSystem(conf)) {
            Path path = fs.getPath("temp");
            FileAttributesChecker checker = new FileAttributesChecker(path);
            MockTerminal terminal = new MockTerminal();
            checker.check(terminal);
            assertTrue(terminal.getOutput(), terminal.getOutput().isEmpty());
        }
    }

    public void testNoChanges() throws Exception {
        Configuration conf = Configuration.unix().toBuilder().setAttributeViews("posix").build();
        try (FileSystem fs = Jimfs.newFileSystem(conf)) {
            Path path = fs.getPath("temp");
            Files.createFile(path);
            FileAttributesChecker checker = new FileAttributesChecker(path);

            MockTerminal terminal = new MockTerminal();
            checker.check(terminal);
            assertTrue(terminal.getOutput(), terminal.getOutput().isEmpty());
        }
    }

    public void testPermissionsChanged() throws Exception {
        Configuration conf = Configuration.unix().toBuilder().setAttributeViews("posix").build();
        try (FileSystem fs = Jimfs.newFileSystem(conf)) {
            Path path = fs.getPath("temp");
            Files.createFile(path);

            PosixFileAttributeView attrs = Files.getFileAttributeView(path, PosixFileAttributeView.class);
            Set<PosixFilePermission> perms = new HashSet<>(attrs.readAttributes().permissions());
            perms.remove(PosixFilePermission.GROUP_READ);
            attrs.setPermissions(perms);

            FileAttributesChecker checker = new FileAttributesChecker(path);
            perms.add(PosixFilePermission.GROUP_READ);
            attrs.setPermissions(perms);

            MockTerminal terminal = new MockTerminal();
            checker.check(terminal);
            String output = terminal.getOutput();
            assertTrue(output, output.contains("permissions of [" + path + "] have changed"));
        }
    }

    public void testOwnerChanged() throws Exception {
        Configuration conf = Configuration.unix().toBuilder().setAttributeViews("posix").build();
        try (FileSystem fs = Jimfs.newFileSystem(conf)) {
            Path path = fs.getPath("temp");
            Files.createFile(path);
            FileAttributesChecker checker = new FileAttributesChecker(path);

            UserPrincipal newOwner = fs.getUserPrincipalLookupService().lookupPrincipalByName("randomuser");
            PosixFileAttributeView attrs = Files.getFileAttributeView(path, PosixFileAttributeView.class);
            attrs.setOwner(newOwner);

            MockTerminal terminal = new MockTerminal();
            checker.check(terminal);
            String output = terminal.getOutput();
            assertTrue(output, output.contains("Owner of file [" + path + "] used to be"));
        }
    }

    public void testGroupChanged() throws Exception {
        Configuration conf = Configuration.unix().toBuilder().setAttributeViews("posix").build();
        try (FileSystem fs = Jimfs.newFileSystem(conf)) {
            Path path = fs.getPath("temp");
            Files.createFile(path);
            FileAttributesChecker checker = new FileAttributesChecker(path);

            GroupPrincipal newGroup = fs.getUserPrincipalLookupService().lookupPrincipalByGroupName("randomgroup");
            PosixFileAttributeView attrs = Files.getFileAttributeView(path, PosixFileAttributeView.class);
            attrs.setGroup(newGroup);

            MockTerminal terminal = new MockTerminal();
            checker.check(terminal);
            String output = terminal.getOutput();
            assertTrue(output, output.contains("Group of file [" + path + "] used to be"));
        }
    }
}
