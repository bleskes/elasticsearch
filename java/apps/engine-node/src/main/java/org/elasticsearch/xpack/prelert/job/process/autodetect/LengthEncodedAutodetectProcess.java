package org.elasticsearch.xpack.prelert.job.process.autodetect;

import org.elasticsearch.xpack.prelert.job.process.autodetect.params.DataLoadParams;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.InterimResultsParams;
import org.elasticsearch.xpack.prelert.job.process.autodetect.writer.ControlMsgToProcessWriter;
import org.elasticsearch.xpack.prelert.job.process.autodetect.writer.LengthEncodedWriter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.ZonedDateTime;

/**
 * Implementation of {@link AutodetectProcess} where all the communication is
 * length encoded strings
 */
public class LengthEncodedAutodetectProcess implements AutodetectProcess {

    private final ZonedDateTime startTime;
    private final LengthEncodedWriter lengthEncodedWriter;
    private final int numberOfAnalysisFields;
    private final ByteArrayInputStream error;
    private final ByteArrayInputStream output;

    public LengthEncodedAutodetectProcess(OutputStream outputStream, int numberOfAnalysisFields) {
        lengthEncodedWriter = new LengthEncodedWriter(outputStream);
        this.numberOfAnalysisFields = numberOfAnalysisFields;
        error = new ByteArrayInputStream(new byte[]{});
        output = new ByteArrayInputStream(new byte[]{});
        startTime = ZonedDateTime.now();
    }

    @Override
    public void writeRecord(String[] record) throws IOException {
        lengthEncodedWriter.writeRecord(record);
    }

    @Override
    public void writeResetBucketsControlMessage(DataLoadParams params) throws IOException {
        ControlMsgToProcessWriter writer = new ControlMsgToProcessWriter(lengthEncodedWriter, numberOfAnalysisFields);
        writer.writeResetBucketsMessage(params);
    }

    @Override
    public void writeUpdateConfigMessage(String config) throws IOException {
        // TODO the config param should be typed
        ControlMsgToProcessWriter writer = new ControlMsgToProcessWriter(lengthEncodedWriter, numberOfAnalysisFields);
        writer.writeUpdateConfigMessage(config);
    }

    @Override
    public void flushJob(InterimResultsParams params) throws IOException {
        ControlMsgToProcessWriter writer = new ControlMsgToProcessWriter(lengthEncodedWriter, numberOfAnalysisFields);
        writer.writeCalcInterimMessage(params);
        writer.writeFlushMessage();
     }

    @Override
    public InputStream error() {
        return error;
    }

    @Override
    public InputStream out() {
        return output;
    }

    @Override
    public ZonedDateTime getProcessStartTime() {
        return startTime;
    }
}
