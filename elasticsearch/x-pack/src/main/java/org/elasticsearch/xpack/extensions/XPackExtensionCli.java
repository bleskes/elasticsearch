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

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.varia.NullAppender;
import org.elasticsearch.cli.MultiCommand;
import org.elasticsearch.cli.Terminal;

/**
 * A cli tool for adding, removing and listing extensions for x-pack.
 */
public class XPackExtensionCli extends MultiCommand {

    public XPackExtensionCli() {
        super("A tool for managing installed x-pack extensions");
        subcommands.put("list", new ListXPackExtensionCommand());
        subcommands.put("install", new InstallXPackExtensionCommand());
        subcommands.put("remove", new RemoveXPackExtensionCommand());
    }

    public static void main(String[] args) throws Exception {
        BasicConfigurator.configure(new NullAppender());
        exit(new XPackExtensionCli().main(args, Terminal.DEFAULT));
    }

}
