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

package org.elasticsearch.shield.key.tool;

import org.elasticsearch.common.cli.CliTool;
import org.elasticsearch.common.cli.CliToolConfig;
import org.elasticsearch.common.cli.Terminal;
import org.elasticsearch.common.cli.commons.CommandLine;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.shield.key.InternalKeyService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static org.elasticsearch.common.cli.CliToolConfig.Builder.cmd;
import static org.elasticsearch.common.cli.CliToolConfig.config;

/**
 *
 */
public class SystemKeyTool extends CliTool {

    public static void main(String[] args) throws Exception {
        int status = new SystemKeyTool().execute(args);
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
        return Generate.parse(terminal, commandLine);
    }

    static class Generate extends Command {
        private static final String NAME = "generate";
        private static final CliToolConfig.Cmd CMD = cmd(NAME, Generate.class).build();

        final Path path;

        Generate(Terminal terminal, Path path) {
            super(terminal);
            this.path = path;
        }

        public static Command parse(Terminal terminal, CommandLine cl) {
            String[] args = cl.getArgs();
            if (args.length > 1) {
                return exitCmd(ExitStatus.USAGE, terminal, "Too many arguments");
            }
            Path path = args.length != 0 ? Paths.get(args[0]) : null;
            return new Generate(terminal, path);
        }

        @Override
        public ExitStatus execute(Settings settings, Environment env) throws Exception {
            Path path = this.path;
            if (path == null) {
                path = InternalKeyService.resolveFile(settings, env);
            }
            terminal.println(Terminal.Verbosity.VERBOSE, "generating...");
            byte[] key = InternalKeyService.generateKey();
            terminal.println("Storing generated key in [%s]", path.toAbsolutePath());
            Files.write(path, key, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return ExitStatus.OK;
        }
    }

}