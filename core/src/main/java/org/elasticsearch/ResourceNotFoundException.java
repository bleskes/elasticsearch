package org.elasticsearch;

import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.rest.RestStatus;

import java.io.IOException;

/**
 * Generic ResourceNotFoundException corresponding to the {@link RestStatus#NOT_FOUND} status code
 */
public class ResourceNotFoundException extends ElasticsearchException {


    public ResourceNotFoundException(ShardId shardId, String msg, Object... args) {
        this(msg, args);
        addShard(shardId);
    }

    public ResourceNotFoundException(ShardId shardId, String msg, Throwable cause, Object... args) {
        this(msg, cause, args);
        addShard(shardId);
    }

    public ResourceNotFoundException(String index, String msg, Object... args) {
        this(msg, args);
        addIndex(index);
    }

    public ResourceNotFoundException(String index, String msg, Throwable cause,  Object... args) {
        this(msg, cause, args);
        addIndex(index);
    }

    public ResourceNotFoundException(String msg, Object... args) {
        super(msg, args);
    }

    public ResourceNotFoundException(String msg, Throwable cause, Object... args) {
        super(msg, cause, args);
    }

    public ResourceNotFoundException(StreamInput in) throws IOException {
        super(in);
    }

    @Override
    public final RestStatus status() {
        return RestStatus.NOT_FOUND;
    }
}
