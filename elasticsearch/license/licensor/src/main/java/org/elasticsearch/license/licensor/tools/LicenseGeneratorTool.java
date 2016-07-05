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
package org.elasticsearch.license.licensor.tools;

import java.nio.file.Files;
import java.nio.file.Path;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.elasticsearch.cli.Command;
import org.elasticsearch.cli.ExitCodes;
import org.elasticsearch.cli.UserException;
import org.elasticsearch.cli.Terminal;
import org.elasticsearch.common.SuppressForbidden;
import org.elasticsearch.common.io.PathUtils;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.license.core.License;
import org.elasticsearch.license.licensor.LicenseSigner;

public class LicenseGeneratorTool extends Command {

    private final OptionSpec<String> publicKeyPathOption;
    private final OptionSpec<String> privateKeyPathOption;
    private final OptionSpec<String> licenseOption;
    private final OptionSpec<String> licenseFileOption;

    public LicenseGeneratorTool() {
        super("Generates signed elasticsearch license(s) for a given license spec(s)");
        publicKeyPathOption = parser.accepts("publicKeyPath", "path to public key file")
            .withRequiredArg().required();
        privateKeyPathOption = parser.accepts("privateKeyPath", "path to private key file")
            .withRequiredArg().required();
        // TODO: with jopt-simple 5.0, we can make these requiredUnless each other
        // which is effectively "one must be present"
        licenseOption = parser.accepts("license", "license json spec")
            .withRequiredArg();
        licenseFileOption = parser.accepts("licenseFile", "license json spec file")
            .withRequiredArg();
    }

    public static void main(String[] args) throws Exception {
        exit(new LicenseGeneratorTool().main(args, Terminal.DEFAULT));
    }

    @Override
    protected void printAdditionalHelp(Terminal terminal) {
        terminal.println("This tool generate elasticsearch license(s) for the provided");
        terminal.println("license spec(s). The tool can take arbitrary number of");
        terminal.println("`--license` and/or `--licenseFile` to generate corresponding");
        terminal.println("signed license(s).");
        terminal.println("");
    }

    @Override
    protected void execute(Terminal terminal, OptionSet options) throws Exception {
        Path publicKeyPath = parsePath(publicKeyPathOption.value(options));
        Path privateKeyPath = parsePath(privateKeyPathOption.value(options));
        if (Files.exists(privateKeyPath) == false) {
            throw new UserException(ExitCodes.USAGE, privateKeyPath + " does not exist");
        } else if (Files.exists(publicKeyPath) == false) {
            throw new UserException(ExitCodes.USAGE, publicKeyPath + " does not exist");
        }

        final License licenseSpec;
        if (options.has(licenseOption)) {
            licenseSpec = License.fromSource(licenseOption.value(options));
        } else if (options.has(licenseFileOption)) {
            Path licenseSpecPath = parsePath(licenseFileOption.value(options));
            if (Files.exists(licenseSpecPath) == false) {
                throw new UserException(ExitCodes.USAGE, licenseSpecPath + " does not exist");
            }
            licenseSpec = License.fromSource(Files.readAllBytes(licenseSpecPath));
        } else {
            throw new UserException(ExitCodes.USAGE, "Must specify either --license or --licenseFile");
        }

        // sign
        License license = new LicenseSigner(privateKeyPath, publicKeyPath).sign(licenseSpec);

        // dump
        XContentBuilder builder = XContentFactory.contentBuilder(XContentType.JSON);
        builder.startObject();
        builder.startObject("license");
        license.toInnerXContent(builder, ToXContent.EMPTY_PARAMS);
        builder.endObject();
        builder.endObject();
        builder.flush();
        terminal.println(builder.string());
    }

    @SuppressForbidden(reason = "Parsing command line path")
    private static Path parsePath(String path) {
        return PathUtils.get(path);
    }
}
