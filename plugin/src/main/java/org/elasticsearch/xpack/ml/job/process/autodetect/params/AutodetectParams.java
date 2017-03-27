/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2017 Elasticsearch BV. All Rights Reserved.
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
package org.elasticsearch.xpack.ml.job.process.autodetect.params;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.xpack.ml.job.config.MlFilter;
import org.elasticsearch.xpack.ml.job.process.autodetect.state.DataCounts;
import org.elasticsearch.xpack.ml.job.process.autodetect.state.ModelSizeStats;
import org.elasticsearch.xpack.ml.job.process.autodetect.state.ModelSnapshot;
import org.elasticsearch.xpack.ml.job.process.autodetect.state.Quantiles;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class AutodetectParams {

    private final DataCounts dataCounts;
    private final ModelSizeStats modelSizeStats;

    @Nullable
    private final ModelSnapshot modelSnapshot;

    @Nullable
    private final Quantiles quantiles;

    private final Set<MlFilter> filters;

    private AutodetectParams(DataCounts dataCounts, ModelSizeStats modelSizeStats,
                             @Nullable ModelSnapshot modelSnapshot,
                             @Nullable Quantiles quantiles, Set<MlFilter> filters) {
        this.dataCounts = Objects.requireNonNull(dataCounts);
        this.modelSizeStats = Objects.requireNonNull(modelSizeStats);
        this.modelSnapshot = modelSnapshot;
        this.quantiles = quantiles;
        this.filters = filters;
    }

    public DataCounts dataCounts() {
        return dataCounts;
    }

    public ModelSizeStats modelSizeStats() {
        return modelSizeStats;
    }

    @Nullable
    public ModelSnapshot modelSnapshot() {
        return modelSnapshot;
    }

    @Nullable
    public Quantiles quantiles() {
        return quantiles;
    }

    public Set<MlFilter> filters() {
        return filters;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other instanceof AutodetectParams == false) {
            return false;
        }

        AutodetectParams that = (AutodetectParams) other;

        return Objects.equals(this.dataCounts, that.dataCounts)
                && Objects.equals(this.modelSizeStats, that.modelSizeStats)
                && Objects.equals(this.modelSnapshot, that.modelSnapshot)
                && Objects.equals(this.quantiles, that.quantiles)
                && Objects.equals(this.filters, that.filters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataCounts, modelSizeStats, modelSnapshot, quantiles, filters);
    }

    public static class Builder {

        private DataCounts dataCounts;
        private ModelSizeStats modelSizeStats;
        private ModelSnapshot modelSnapshot;
        private Quantiles quantiles;
        private Set<MlFilter> filters;

        public Builder(String jobId) {
            dataCounts = new DataCounts(jobId);
            modelSizeStats = new ModelSizeStats.Builder(jobId).build();
            filters = new HashSet<>();
        }

        public Builder setDataCounts(DataCounts dataCounts) {
            this.dataCounts = dataCounts;
            return this;
        }

        public Builder setModelSizeStats(ModelSizeStats modelSizeStats) {
            this.modelSizeStats = modelSizeStats;
            return this;
        }

        public Builder setModelSnapshot(ModelSnapshot modelSnapshot) {
            this.modelSnapshot = modelSnapshot;
            return this;
        }

        public Builder setQuantiles(Quantiles quantiles) {
            this.quantiles = quantiles;
            return this;
        }

        public Builder addFilter(MlFilter filter) {
            filters.add(filter);
            return this;
        }

        public Builder setFilters(Set<MlFilter> filters) {
            filters = Objects.requireNonNull(filters);
            return this;
        }

        public AutodetectParams build() {
            return new AutodetectParams(dataCounts, modelSizeStats, modelSnapshot, quantiles,
                    filters);
        }
    }
}
