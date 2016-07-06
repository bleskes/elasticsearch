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

package org.elasticsearch.xpack.security.crypto.tool;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.elasticsearch.cli.ExitCodes;
import org.elasticsearch.cli.SettingCommand;
import org.elasticsearch.cli.Terminal;
import org.elasticsearch.cli.UserException;
import org.elasticsearch.common.SuppressForbidden;
import org.elasticsearch.common.io.PathUtils;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.set.Sets;
import org.elasticsearch.env.Environment;
import org.elasticsearch.node.internal.InternalSettingsPreparer;
import org.elasticsearch.xpack.security.crypto.InternalCryptoService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SystemKeyTool extends SettingCommand {

    private final OptionSpec<String> arguments;

    SystemKeyTool() {
        super("system key tool");
        arguments = parser.nonOptions("key path");
    }

    public static final Set<PosixFilePermission> PERMISSION_OWNER_READ_WRITE = Sets.newHashSet(PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE);

    public static void main(String[] args) throws Exception {
        final SystemKeyTool tool = new SystemKeyTool();
        int status = main(tool, args, Terminal.DEFAULT);
        if (status != ExitCodes.OK) {
            exit(status);
        }
    }

    static int main(SystemKeyTool tool, String[] args, Terminal terminal) throws Exception {
        return tool.main(args, terminal);
    }

    @Override
    protected void execute(Terminal terminal, OptionSet options, Map<String, String> settings) throws Exception {
        final Path keyPath;

        final Environment env = InternalSettingsPreparer.prepareEnvironment(Settings.EMPTY, terminal, settings);

        if (options.hasArgument(arguments)) {
            List<String> args = arguments.values(options);
            if (args.size() > 1) {
                throw new UserException(ExitCodes.USAGE, "No more than one key path can be supplied");
            }
            keyPath = parsePath(args.get(0));
        } else {
            keyPath = InternalCryptoService.resolveSystemKey(env.settings(), env);
        }

        // write the key
        terminal.println(Terminal.Verbosity.VERBOSE, "generating...");
        byte[] key = InternalCryptoService.generateKey();
        terminal.println(String.format(Locale.ROOT, "Storing generated key in [%s]...", keyPath.toAbsolutePath()));
        Files.write(keyPath, key, StandardOpenOption.CREATE_NEW);

        // set permissions to 600
        PosixFileAttributeView view = Files.getFileAttributeView(keyPath, PosixFileAttributeView.class);
        if (view != null) {
            view.setPermissions(PERMISSION_OWNER_READ_WRITE);
            terminal.println("Ensure the generated key can be read by the user that Elasticsearch runs as, "
                + "permissions are set to owner read/write only");
        }
    }

    @SuppressForbidden(reason = "Parsing command line path")
    private static Path parsePath(String path) {
        return PathUtils.get(path);
    }

}
