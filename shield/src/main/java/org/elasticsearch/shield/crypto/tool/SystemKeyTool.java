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

import org.apache.commons.cli.CommandLine;
import org.elasticsearch.common.SuppressForbidden;
import org.elasticsearch.common.cli.CheckFileCommand;
import org.elasticsearch.common.cli.CliTool;
import org.elasticsearch.common.cli.CliToolConfig;
import org.elasticsearch.common.cli.Terminal;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.set.Sets;
import org.elasticsearch.env.Environment;
import org.elasticsearch.shield.crypto.InternalCryptoService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import static org.elasticsearch.common.cli.CliToolConfig.Builder.cmd;
import static org.elasticsearch.common.cli.CliToolConfig.config;

/**
 *
 */
public class SystemKeyTool extends CliTool {

    public static final Set<PosixFilePermission> PERMISSION_OWNER_READ_WRITE = Sets.newHashSet(PosixFilePermission.OWNER_READ,
        PosixFilePermission.OWNER_WRITE);


    public static void main(String[] args) throws Exception {
        ExitStatus exitStatus = new SystemKeyTool().execute(args);
        exit(exitStatus.status());
    }

    @SuppressForbidden(reason = "Allowed to exit explicitly from #main()")
    private static void exit(int status) {
        System.exit(status);
    }

    private static final CliToolConfig CONFIG = config("syskey", SystemKeyTool.class)
            .cmds(Generate.CMD)
            .build();

    public SystemKeyTool() {
        super(CONFIG);
    }

    @Override
    protected Command parse(String cmd, CommandLine commandLine) throws Exception {
        return Generate.parse(terminal, commandLine, env);
    }

    static class Generate extends CheckFileCommand {

        private static final CliToolConfig.Cmd CMD = cmd("generate", Generate.class).build();

        final Path path;

        Generate(Terminal terminal, Path path) {
            super(terminal);
            this.path = path;
        }

        public static Command parse(Terminal terminal, CommandLine cl, Environment env) {
            String[] args = cl.getArgs();
            if (args.length > 1) {
                return exitCmd(ExitStatus.USAGE, terminal, "Too many arguments");
            }
            Path path = args.length != 0 ? env.binFile().getParent().resolve(args[0]) : null;
            return new Generate(terminal, path);
        }

        @Override
        protected Path[] pathsForPermissionsCheck(Settings settings, Environment env) {
            Path path = this.path;
            if (path == null) {
                path = InternalCryptoService.resolveSystemKey(settings, env);
            }
            return new Path[] { path };
        }

        @Override
        public ExitStatus doExecute(Settings settings, Environment env) throws Exception {
            Path path = this.path;
            try {
                if (path == null) {
                    path = InternalCryptoService.resolveSystemKey(settings, env);
                }
                terminal.println(Terminal.Verbosity.VERBOSE, "generating...");
                byte[] key = InternalCryptoService.generateKey();
                terminal.println("Storing generated key in [%s]...", path.toAbsolutePath());
                Files.write(path, key, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException ioe) {
                terminal.printError("Cannot generate and save system key file [%s]", path.toAbsolutePath());
                return ExitStatus.IO_ERROR;
            }

            boolean supportsPosixPermissions = Environment.getFileStore(path).supportsFileAttributeView(PosixFileAttributeView.class);
            if (supportsPosixPermissions) {
                try {
                    Files.setPosixFilePermissions(path, PERMISSION_OWNER_READ_WRITE);
                } catch (IOException ioe) {
                    terminal.printError("Cannot set owner read/write permissions to generated system key file [%s]", path.toAbsolutePath());
                    return ExitStatus.IO_ERROR;
                }
                terminal.println("Ensure the generated key can be read by the user that Elasticsearch runs as, permissions are set to owner read/write only");
            }

            return ExitStatus.OK;
        }
    }

}
