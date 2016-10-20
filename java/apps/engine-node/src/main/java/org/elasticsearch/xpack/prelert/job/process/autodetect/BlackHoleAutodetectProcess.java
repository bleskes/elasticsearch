package org.elasticsearch.xpack.prelert.job.process.autodetect;

import org.elasticsearch.xpack.prelert.job.process.autodetect.params.DataLoadParams;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.InterimResultsParams;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;

public class BlackHoleAutodetectProcess implements AutodetectProcess {

    private final InputStream error;
    private final InputStream out;
    private final ZonedDateTime startTime;

    public BlackHoleAutodetectProcess() {
        error = new ByteArrayInputStream(new byte[] {});
        out = new ByteArrayInputStream(new byte[] {});
        startTime = ZonedDateTime.now();
    }

    @Override
    public void writeRecord(String[] record) throws IOException {
    }

    @Override
    public void writeResetBucketsControlMessage(DataLoadParams params) throws IOException {
    }

    @Override
    public void writeUpdateConfigMessage(String config) throws IOException {
    }

    @Override
    public void flushJob(InterimResultsParams params) throws IOException {
    }

    @Override
    public void flushStream() throws IOException {

    }

    @Override
    public void close() throws IOException {
        error.close();
        out.close();
    }

    @Override
    public InputStream error() {
        return this.error;
    }

    @Override
    public InputStream out() {
        return this.out;
    }

    @Override
    public ZonedDateTime getProcessStartTime() {
        return startTime;
    }
}
