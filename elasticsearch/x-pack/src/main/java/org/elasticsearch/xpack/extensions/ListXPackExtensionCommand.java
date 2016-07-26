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

package org.elasticsearch.xpack.extensions;

import joptsimple.OptionSet;
import org.elasticsearch.cli.SettingCommand;
import org.elasticsearch.cli.Terminal;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.node.internal.InternalSettingsPreparer;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.cli.Terminal.Verbosity.VERBOSE;
import static org.elasticsearch.xpack.XPackPlugin.resolveXPackExtensionsFile;

/**
 * A command for the extension cli to list extensions installed in x-pack.
 */
class ListXPackExtensionCommand extends SettingCommand {

    ListXPackExtensionCommand() {
        super("Lists installed x-pack extensions");
    }

    @Override
    protected void execute(Terminal terminal, OptionSet options, Map<String, String> settings) throws Exception {
        Environment env = InternalSettingsPreparer.prepareEnvironment(Settings.EMPTY, terminal, settings);
        if (Files.exists(resolveXPackExtensionsFile(env)) == false) {
            throw new IOException("Extensions directory missing: " + resolveXPackExtensionsFile(env));
        }
        terminal.println(VERBOSE, "XPack Extensions directory: " + resolveXPackExtensionsFile(env));
        final List<Path> extensions = new ArrayList<>();
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(resolveXPackExtensionsFile(env))) {
            for (Path extension : paths) {
                extensions.add(extension);
            }
        }
        Collections.sort(extensions);
        for (final Path extension : extensions) {
            terminal.println(extension.getFileName().toString());
            XPackExtensionInfo info =
                    XPackExtensionInfo.readFromProperties(extension);
            terminal.println(VERBOSE, info.toString());
        }
    }

}
