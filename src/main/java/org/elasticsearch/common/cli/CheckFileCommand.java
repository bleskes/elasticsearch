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

package org.elasticsearch.common.cli;

import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Map;
import java.util.Set;

import static org.elasticsearch.common.cli.Terminal.Verbosity.SILENT;

/**
 * helper command to check if file permissions or owner got changed by the command being executed
 */
public abstract class CheckFileCommand extends CliTool.Command {

    public CheckFileCommand(Terminal terminal) {
        super(terminal);
    }

    /**
     * abstract method, which should implement the same logic as CliTool.Command.execute(), but is wrapped
     */
    public abstract CliTool.ExitStatus doExecute(Settings settings, Environment env) throws Exception;

    /**
     * Returns the array of paths, that should be checked if the permissions, user or groups have changed
     * before and after execution of the command
     *
     */
    protected abstract Path[] pathsForPermissionsCheck(Settings settings, Environment env) throws Exception;

    @Override
    public CliTool.ExitStatus execute(Settings settings, Environment env) throws Exception {
        Path[] paths = pathsForPermissionsCheck(settings, env);

        Map<Path, Set<PosixFilePermission>> permissions = Maps.newHashMapWithExpectedSize(paths.length);
        Map<Path, String> owners = Maps.newHashMapWithExpectedSize(paths.length);
        Map<Path, String> groups = Maps.newHashMapWithExpectedSize(paths.length);

        if (paths != null && paths.length > 0) {
            for (Path path : paths) {
                try {
                    boolean supportsPosixPermissions = Files.getFileStore(path).supportsFileAttributeView(PosixFileAttributeView.class);
                    if (supportsPosixPermissions) {
                        permissions.put(path, Files.getPosixFilePermissions(path));
                        owners.put(path, Files.getOwner(path).getName());
                        groups.put(path, Files.readAttributes(path, PosixFileAttributes.class).group().getName());
                    }
                } catch (IOException e) {
                    // silently swallow if not supported, no need to log things
                }
            }
        }

        CliTool.ExitStatus status = doExecute(settings, env);

        // check if permissions differ
        for (Map.Entry<Path, Set<PosixFilePermission>> entry : permissions.entrySet()) {
            Set<PosixFilePermission> permissionsBeforeWrite = entry.getValue();
            Set<PosixFilePermission> permissionsAfterWrite = Files.getPosixFilePermissions(entry.getKey());
            if (!permissionsBeforeWrite.equals(permissionsAfterWrite)) {
                terminal.println(SILENT, "WARN: The file permissions of [%s] have changed from [%s] to [%s]",
                        entry.getKey(), PosixFilePermissions.toString(permissionsBeforeWrite), PosixFilePermissions.toString(permissionsAfterWrite));
                terminal.println(SILENT, "Please ensure that the user account running Elasticsearch has read access to this file!");
            }
        }

        // check if owner differs
        for (Map.Entry<Path, String> entry : owners.entrySet()) {
            String ownerBeforeWrite = entry.getValue();
            String ownerAfterWrite = Files.getOwner(entry.getKey()).getName();
            if (!ownerAfterWrite.equals(ownerBeforeWrite)) {
                terminal.println(SILENT, "WARN: Owner of file [%s] used to be [%s], but now is [%s]", entry.getKey(), ownerBeforeWrite, ownerAfterWrite);
            }
        }

        // check if group differs
        for (Map.Entry<Path, String> entry : groups.entrySet()) {
            String groupBeforeWrite = entry.getValue();
            String groupAfterWrite = Files.readAttributes(entry.getKey(), PosixFileAttributes.class).group().getName();
            if (!groupAfterWrite.equals(groupBeforeWrite)) {
                terminal.println(SILENT, "WARN: Group of file [%s] used to be [%s], but now is [%s]", entry.getKey(), groupBeforeWrite, groupAfterWrite);
            }
        }

        return status;
    }
}
