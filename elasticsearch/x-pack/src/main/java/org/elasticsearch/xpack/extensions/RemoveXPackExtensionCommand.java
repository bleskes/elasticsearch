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

package org.elasticsearch.xpack.extensions;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.apache.lucene.util.IOUtils;
import org.elasticsearch.cli.ExitCodes;
import org.elasticsearch.cli.SettingCommand;
import org.elasticsearch.cli.Terminal;
import org.elasticsearch.cli.UserError;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.node.internal.InternalSettingsPreparer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.cli.Terminal.Verbosity.VERBOSE;
import static org.elasticsearch.xpack.XPackPlugin.resolveXPackExtensionsFile;

/**
 * A command for the extension cli to remove an extension from x-pack.
 */
class RemoveXPackExtensionCommand  extends SettingCommand {
    private final OptionSpec<String> arguments;

    RemoveXPackExtensionCommand() {
        super("Removes an extension from x-pack");
        this.arguments = parser.nonOptions("extension name");
    }

    @Override
    protected void execute(Terminal terminal, OptionSet options, Map<String, String> settings) throws Exception {

        // TODO: in jopt-simple 5.0 we can enforce a min/max number of positional args
        List<String> args = arguments.values(options);
        if (args.size() != 1) {
            throw new UserError(ExitCodes.USAGE, "Must supply a single extension id argument");
        }
        execute(terminal, args.get(0), settings);
    }

    // pkg private for testing
    void execute(Terminal terminal, String extensionName, Map<String, String> settings) throws Exception {
        terminal.println("-> Removing " + Strings.coalesceToEmpty(extensionName) + "...");

        Environment env = InternalSettingsPreparer.prepareEnvironment(Settings.EMPTY, terminal, settings);
        Path extensionDir = resolveXPackExtensionsFile(env).resolve(extensionName);
        if (Files.exists(extensionDir) == false) {
            throw new UserError(ExitCodes.USAGE,
                    "Extension " + extensionName + " not found. Run 'bin/x-pack/extension list' to get list of installed extensions.");
        }

        List<Path> extensionPaths = new ArrayList<>();

        terminal.println(VERBOSE, "Removing: " + extensionDir);
        Path tmpExtensionDir = resolveXPackExtensionsFile(env).resolve(".removing-" + extensionName);
        Files.move(extensionDir, tmpExtensionDir, StandardCopyOption.ATOMIC_MOVE);
        extensionPaths.add(tmpExtensionDir);

        IOUtils.rm(extensionPaths.toArray(new Path[extensionPaths.size()]));
    }
}
