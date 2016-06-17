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

package org.elasticsearch.xpack.security.support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;

import org.elasticsearch.cli.Terminal;

/**
 * A utility for cli tools to capture file attributes
 * before writing files, and to warn if the permissions/group/owner changes.
 */
public class FileAttributesChecker {

    // the paths to check
    private final Path[] paths;

    // captured attributes for each path
    private final PosixFileAttributes[] attributes;

    /** Create a checker for the given paths, which will warn to the given terminal if changes are made. */
    public FileAttributesChecker(Path... paths) throws IOException {
        this.paths = paths;
        this.attributes = new PosixFileAttributes[paths.length];

        for (int i = 0; i < paths.length; ++i) {
            if (Files.exists(paths[i]) == false) continue; // missing file, so changes later don't matter
            PosixFileAttributeView view = Files.getFileAttributeView(paths[i], PosixFileAttributeView.class);
            if (view == null) continue; // not posix
            this.attributes[i] = view.readAttributes();
        }
    }

    /** Check if attributes of the paths have changed, warning to the given terminal if they have. */
    public void check(Terminal terminal) throws IOException {
        for (int i = 0; i < paths.length; ++i) {
            if (attributes[i] == null) {
                // we couldn't get attributes in setup, so we can't check them now
                continue;
            }

            PosixFileAttributeView view = Files.getFileAttributeView(paths[i], PosixFileAttributeView.class);
            PosixFileAttributes newAttributes = view.readAttributes();
            PosixFileAttributes oldAttributes = attributes[i];
            if (oldAttributes.permissions().equals(newAttributes.permissions()) == false) {
                terminal.println(Terminal.Verbosity.SILENT, "WARNING: The file permissions of [" + paths[i] + "] have changed "
                    + "from [" + PosixFilePermissions.toString(oldAttributes.permissions()) + "] "
                    + "to [" + PosixFilePermissions.toString(newAttributes.permissions()) + "]");
                terminal.println(Terminal.Verbosity.SILENT,
                    "Please ensure that the user account running Elasticsearch has read access to this file!");
            }
            if (oldAttributes.owner().getName().equals(newAttributes.owner().getName()) == false) {
                terminal.println(Terminal.Verbosity.SILENT, "WARNING: Owner of file [" + paths[i] + "] "
                    + "used to be [" + oldAttributes.owner().getName() + "], "
                    + "but now is [" + newAttributes.owner().getName() + "]");
            }
            if (oldAttributes.group().getName().equals(newAttributes.group().getName()) == false) {
                terminal.println(Terminal.Verbosity.SILENT, "WARNING: Group of file [" + paths[i] + "] "
                    + "used to be [" + oldAttributes.group().getName() + "], "
                    + "but now is [" + newAttributes.group().getName() + "]");
            }
        }
    }
}
