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

import org.apache.logging.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.env.Environment;
import org.elasticsearch.xpack.ml.job.process.logging.CppLogMessageHandler;
import org.elasticsearch.xpack.ml.utils.NamedPipeHelper;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Maintains the connection to the native controller daemon that can start other processes.
 */
public class NativeController {
    private static final Logger LOGGER = Loggers.getLogger(NativeController.class);

    // The controller process should already be running by the time this class tries to connect to it, so the timeout can be short
    private static final Duration CONTROLLER_CONNECT_TIMEOUT = Duration.ofSeconds(2);

    private static final String START_COMMAND = "start";

    public static final Map<String, Object> UNKNOWN_NATIVE_CODE_INFO;

    static {
        Map<String, Object> unknownInfo = new HashMap<>(2);
        unknownInfo.put("version", "N/A");
        unknownInfo.put("build_hash", "N/A");
        UNKNOWN_NATIVE_CODE_INFO = Collections.unmodifiableMap(unknownInfo);
    }

    private final CppLogMessageHandler cppLogHandler;
    private final OutputStream commandStream;
    private Thread logTailThread;

    public NativeController(Environment env, NamedPipeHelper namedPipeHelper) throws IOException {
        ProcessPipes processPipes = new ProcessPipes(env, namedPipeHelper, ProcessCtrl.CONTROLLER, null,
                true, true, false, false, false, false);
        processPipes.connectStreams(CONTROLLER_CONNECT_TIMEOUT);
        cppLogHandler = new CppLogMessageHandler(null, processPipes.getLogStream().get());
        commandStream = processPipes.getCommandStream().get();
    }

    public void tailLogsInThread() {
        logTailThread = new Thread(() -> {
            try {
                cppLogHandler.tailStream();
                cppLogHandler.close();
            } catch (IOException e) {
                LOGGER.error("Error tailing C++ controller logs", e);
            }
            LOGGER.info("Native controller process has stopped - no new native processes can be started");
        });
        logTailThread.start();
    }

    public long getPid() throws TimeoutException {
        return cppLogHandler.getPid(CONTROLLER_CONNECT_TIMEOUT);
    }

    public Map<String, Object> getNativeCodeInfo() throws TimeoutException {
        String copyrightMessage = cppLogHandler.getCppCopyright(CONTROLLER_CONNECT_TIMEOUT);
        Matcher matcher = Pattern.compile("Version (.+) \\(Build ([0-9a-f]+)\\) Copyright ").matcher(copyrightMessage);
        if (matcher.find()) {
            Map<String, Object> info = new HashMap<>(2);
            info.put("version", matcher.group(1));
            info.put("build_hash", matcher.group(2));
            return info;
        } else {
            // If this happens it probably means someone has changed the format in lib/ver/CBuildInfo.cc
            // in the machine-learning-cpp repo without changing the pattern above to match
            String msg = "Unexpected native controller process copyright format: " + copyrightMessage;
            LOGGER.error(msg);
            throw new ElasticsearchException(msg);
        }
    }

    public void startProcess(List<String> command) throws IOException {
        // Sanity check to avoid hard-to-debug errors - tabs and newlines will confuse the controller process
        for (String arg : command) {
            if (arg.contains("\t")) {
                throw new IllegalArgumentException("argument contains a tab character: " + arg + " in " + command);
            }
            if (arg.contains("\n")) {
                throw new IllegalArgumentException("argument contains a newline character: " + arg + " in " + command);
            }
        }

        synchronized (commandStream) {
            LOGGER.debug("Starting process with command: " + command);
            commandStream.write(START_COMMAND.getBytes(StandardCharsets.UTF_8));
            for (String arg : command) {
                commandStream.write('\t');
                commandStream.write(arg.getBytes(StandardCharsets.UTF_8));
            }
            commandStream.write('\n');
        }
    }
}
