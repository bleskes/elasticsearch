/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.shield.support;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.elasticsearch.common.cli.CliToolTestCase;
import org.elasticsearch.test.ESTestCase;

public class FileAttributesCheckerTests extends ESTestCase {

    public void testNonExistentFile() throws Exception {
        Path path = createTempDir().resolve("dne");
        FileAttributesChecker checker = new FileAttributesChecker(path);
        CliToolTestCase.CaptureOutputTerminal terminal = new CliToolTestCase.CaptureOutputTerminal();
        checker.check(terminal);
        assertTrue(terminal.getTerminalOutput().isEmpty());
    }

    public void testNoPosix() throws Exception {
        Configuration conf = Configuration.unix().toBuilder().setAttributeViews("basic").build();
        try (FileSystem fs = Jimfs.newFileSystem(conf)) {
            Path path = fs.getPath("temp");
            FileAttributesChecker checker = new FileAttributesChecker(path);
            CliToolTestCase.CaptureOutputTerminal terminal = new CliToolTestCase.CaptureOutputTerminal();
            checker.check(terminal);
            assertTrue(terminal.getTerminalOutput().isEmpty());
        }
    }

    public void testNoChanges() throws Exception {
        Configuration conf = Configuration.unix().toBuilder().setAttributeViews("posix").build();
        try (FileSystem fs = Jimfs.newFileSystem(conf)) {
            Path path = fs.getPath("temp");
            Files.createFile(path);
            FileAttributesChecker checker = new FileAttributesChecker(path);

            CliToolTestCase.CaptureOutputTerminal terminal = new CliToolTestCase.CaptureOutputTerminal();
            checker.check(terminal);
            assertTrue(terminal.getTerminalOutput().isEmpty());
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

            CliToolTestCase.CaptureOutputTerminal terminal = new CliToolTestCase.CaptureOutputTerminal();
            checker.check(terminal);
            List<String> output = terminal.getTerminalOutput();
            assertEquals(output.toString(), 2, output.size());
            assertTrue(output.toString(), output.get(0).contains("permissions of [" + path + "] have changed"));
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

            CliToolTestCase.CaptureOutputTerminal terminal = new CliToolTestCase.CaptureOutputTerminal();
            checker.check(terminal);
            List<String> output = terminal.getTerminalOutput();
            assertEquals(output.toString(), 1, output.size());
            assertTrue(output.toString(), output.get(0).contains("Owner of file [" + path + "] used to be"));
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

            CliToolTestCase.CaptureOutputTerminal terminal = new CliToolTestCase.CaptureOutputTerminal();
            checker.check(terminal);
            List<String> output = terminal.getTerminalOutput();
            assertEquals(output.toString(), 1, output.size());
            assertTrue(output.toString(), output.get(0).contains("Group of file [" + path + "] used to be"));
        }
    }
}
