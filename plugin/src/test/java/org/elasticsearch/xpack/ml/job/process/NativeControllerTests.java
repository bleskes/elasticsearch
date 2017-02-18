/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.ml.job.process;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.ml.utils.NamedPipeHelper;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.when;

public class NativeControllerTests extends ESTestCase {

    private Settings settings = Settings.builder().put(Environment.PATH_HOME_SETTING.getKey(), createTempDir().toString()).build();

    public void testStartProcessCommand() throws IOException {

        NamedPipeHelper namedPipeHelper = Mockito.mock(NamedPipeHelper.class);
        ByteArrayInputStream logStream = new ByteArrayInputStream(new byte[1]);
        when(namedPipeHelper.openNamedPipeInputStream(contains("log"), any(Duration.class)))
                .thenReturn(logStream);
        ByteArrayOutputStream commandStream = new ByteArrayOutputStream();
        when(namedPipeHelper.openNamedPipeOutputStream(contains("command"), any(Duration.class)))
                .thenReturn(commandStream);

        List<String> command = new ArrayList<>();
        command.add("my_process");
        command.add("--arg1");
        command.add("--arg2=42");
        command.add("--arg3=something with spaces");

        NativeController nativeController = new NativeController(new Environment(settings), namedPipeHelper);
        nativeController.startProcess(command);

        assertEquals("start\tmy_process\t--arg1\t--arg2=42\t--arg3=something with spaces\n",
                commandStream.toString(StandardCharsets.UTF_8.name()));
    }

    public void testGetNativeCodeInfo() throws IOException, TimeoutException {

        String testMessage = "{\"logger\":\"controller\",\"timestamp\":1478261151445,\"level\":\"INFO\",\"pid\":10211,"
                + "\"thread\":\"0x7fff7d2a8000\",\"message\":\"controller (64 bit): Version 6.0.0-alpha1-SNAPSHOT (Build a0d6ef8819418c) "
                + "Copyright (c) 2017 Elasticsearch BV\",\"method\":\"main\",\"file\":\"Main.cc\",\"line\":123}\n";

        NamedPipeHelper namedPipeHelper = Mockito.mock(NamedPipeHelper.class);
        ByteArrayInputStream logStream = new ByteArrayInputStream(testMessage.getBytes(StandardCharsets.UTF_8));
        when(namedPipeHelper.openNamedPipeInputStream(contains("log"), any(Duration.class)))
                .thenReturn(logStream);
        ByteArrayOutputStream commandStream = new ByteArrayOutputStream();
        when(namedPipeHelper.openNamedPipeOutputStream(contains("command"), any(Duration.class)))
                .thenReturn(commandStream);

        NativeController nativeController = new NativeController(new Environment(settings), namedPipeHelper);
        nativeController.tailLogsInThread();
        Map<String, Object> nativeCodeInfo = nativeController.getNativeCodeInfo();

        assertNotNull(nativeCodeInfo);
        assertEquals(2, nativeCodeInfo.size());
        assertEquals("6.0.0-alpha1-SNAPSHOT", nativeCodeInfo.get("version"));
        assertEquals("a0d6ef8819418c", nativeCodeInfo.get("build_hash"));
    }
}
