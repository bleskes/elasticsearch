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

import org.apache.lucene.util.LuceneTestCase;
import org.elasticsearch.Version;
import org.elasticsearch.cli.ExitCodes;
import org.elasticsearch.cli.MockTerminal;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.test.ESTestCase;
import org.junit.Before;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

@LuceneTestCase.SuppressFileSystems("*")
public class ListXPackExtensionCommandTests extends ESTestCase {

    private Path home;
    private Environment env;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        home = createTempDir();
        Settings settings = Settings.builder()
                .put("path.home", home)
                .build();
        env = new Environment(settings);
        Files.createDirectories(extensionsFile(env));
    }

    static String buildMultiline(String... args){
        return Arrays.asList(args).stream().collect(Collectors.joining("\n", "", "\n"));
    }

    static void buildFakeExtension(Environment env, String description, String name, String className) throws IOException {
        XPackExtensionTestUtil.writeProperties(extensionsFile(env).resolve(name),
                "description", description,
                "name", name,
                "version", "1.0",
                "xpack.version", Version.CURRENT.toString(),
                "java.version", System.getProperty("java.specification.version"),
                "classname", className);
    }

    static Path extensionsFile(final Environment env) throws IOException {
        return env.pluginsFile().resolve("x-pack").resolve("extensions");
    }

    static MockTerminal listExtensions(Path home) throws Exception {
        MockTerminal terminal = new MockTerminal();
        int status = new ListXPackExtensionCommand().main(new String[] { "-Epath.home=" + home }, terminal);
        assertEquals(ExitCodes.OK, status);
        return terminal;
    }

    static MockTerminal listExtensions(Path home, String[] args) throws Exception {
        String[] argsAndHome = new String[args.length + 1];
        System.arraycopy(args, 0, argsAndHome, 0, args.length);
        argsAndHome[args.length] = "-Epath.home=" + home;
        MockTerminal terminal = new MockTerminal();
        int status = new ListXPackExtensionCommand().main(argsAndHome, terminal);
        assertEquals(ExitCodes.OK, status);
        return terminal;
    }

    public void testExtensionsDirMissing() throws Exception {
        Files.delete(extensionsFile(env));
        IOException e = expectThrows(IOException.class, () -> listExtensions(home));
        assertTrue(e.getMessage(), e.getMessage().contains("Extensions directory missing"));
    }

    public void testNoExtensions() throws Exception {
        MockTerminal terminal = listExtensions(home);
        assertTrue(terminal.getOutput(), terminal.getOutput().isEmpty());
    }

    public void testNoExtensionsVerbose() throws Exception {
        String[] params = { "-v" };
        MockTerminal terminal = listExtensions(home, params);
        assertEquals(terminal.getOutput(), buildMultiline("XPack Extensions directory: " + extensionsFile(env)));
    }

    public void testOneExtension() throws Exception {
        buildFakeExtension(env, "", "fake", "org.fake");
        MockTerminal terminal = listExtensions(home);
        assertEquals(terminal.getOutput(), buildMultiline("fake"));
    }

    public void testTwoExtensions() throws Exception {
        buildFakeExtension(env, "", "fake1", "org.fake1");
        buildFakeExtension(env, "", "fake2", "org.fake2");
        MockTerminal terminal = listExtensions(home);
        assertEquals(terminal.getOutput(), buildMultiline("fake1", "fake2"));
    }

    public void testExtensionWithVerbose() throws Exception {
        buildFakeExtension(env, "fake desc", "fake_extension", "org.fake");
        String[] params = { "-v" };
        MockTerminal terminal = listExtensions(home, params);
        assertEquals(terminal.getOutput(), buildMultiline("XPack Extensions directory: " + extensionsFile(env),
                "fake_extension", "- XPack Extension information:", "Name: fake_extension",
                "Description: fake desc", "Version: 1.0", " * Classname: org.fake"));
    }

    public void testExtensionWithVerboseMultipleExtensions() throws Exception {
        buildFakeExtension(env, "fake desc 1", "fake_extension1", "org.fake");
        buildFakeExtension(env, "fake desc 2", "fake_extension2", "org.fake2");
        String[] params = { "-v" };
        MockTerminal terminal = listExtensions(home, params);
        assertEquals(terminal.getOutput(), buildMultiline("XPack Extensions directory: " + extensionsFile(env),
                "fake_extension1", "- XPack Extension information:", "Name: fake_extension1",
                "Description: fake desc 1", "Version: 1.0", " * Classname: org.fake",
                "fake_extension2", "- XPack Extension information:", "Name: fake_extension2",
                "Description: fake desc 2", "Version: 1.0", " * Classname: org.fake2"));
    }

    public void testExtensionWithoutVerboseMultipleExtensions() throws Exception {
        buildFakeExtension(env, "fake desc 1", "fake_extension1", "org.fake");
        buildFakeExtension(env, "fake desc 2", "fake_extension2", "org.fake2");
        MockTerminal terminal = listExtensions(home, new String[0]);
        String output = terminal.getOutput();
        assertEquals(output, buildMultiline("fake_extension1", "fake_extension2"));
    }

    public void testExtensionWithoutDescriptorFile() throws Exception{
        Files.createDirectories(extensionsFile(env).resolve("fake1"));
        NoSuchFileException e = expectThrows(NoSuchFileException.class, () -> listExtensions(home));
        assertEquals(e.getFile(),
                extensionsFile(env).resolve("fake1").resolve(XPackExtensionInfo.XPACK_EXTENSION_PROPERTIES).toString());
    }

    public void testExtensionWithWrongDescriptorFile() throws Exception{
        XPackExtensionTestUtil.writeProperties(extensionsFile(env).resolve("fake1"),
                "description", "fake desc");
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> listExtensions(home));
        assertEquals(e.getMessage(), "Property [name] is missing in [" +
                extensionsFile(env).resolve("fake1")
                        .resolve(XPackExtensionInfo.XPACK_EXTENSION_PROPERTIES).toString() + "]");
    }
}
