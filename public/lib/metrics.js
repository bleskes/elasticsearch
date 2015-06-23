var metrics = {
  'total.search.query_total': {
    label: 'Search Request Rate',
    description: 'The cluster wide rate at which search reqeusts are being executed.',
    format: '0.00',
    metricAgg: 'avg',
    units: 'rps',
    defaults: { warning: '>100', critical: '>5000', interval: '1m', periods: 1 },
    type: 'cluster',
    derivative: true
  },
  'primaries.indexing.index_total': {
    label: 'Indexing Request Rate',
    description: 'The cluster wide rate at which documents are being indexed.',
    format: '0.00',
    metricAgg: 'avg',
    units: 'rps',
    defaults: { warning: '>1000', critical: '>5000', interval: '1m', periods: 1 },
    type: 'cluster',
    derivative: true
  },
  'total.search.query_latency': {
    label: 'Query Latency',
    description: 'The average query latency across the entire cluster.',
    format: '0.00',
    metricAgg: 'sum',
    aggs: {
      query_time_in_millis: {
        avg: { field: 'total.search.query_time_in_millis' }
      },
      query_total: {
        avg: { field: 'total.search.query_total' }
      },
      query_time_in_millis_deriv: {
        derivative: { buckets_path: 'query_time_in_millis', gap_policy: 'insert_zeros' }
      },
      query_total_deriv: {
        derivative: { buckets_path: 'query_total', gap_policy: 'insert_zeros' }
      }
    },
    units: 'ms',
    defaults: { warning: '>100', critical: '>200', interval: '1m', periods: 1 },
    type: 'cluster',
    calculation: function (last) {
      return last.query_time_in_millis_deriv.value / last.query_total_deriv.value;
    }
  },
  'os.cpu.user': {
    label: 'CPU Utilization',
    description: 'The percentage of CPU usage.',
    format: '0.0%',
    metricAgg: 'avg',
    units: '',
    defaults: { warning: '>0.7', critical: '>0.9', interval: '1m', periods: 1 },
    type: 'node',
    derivative: false
  },
  'jvm.mem.heap_used_percent': {
    label: 'JVM Heap Usage',
    description: 'The amound of heap used by the JVM',
    format: '0.0%',
    metricAgg: 'avg',
    units: '',
    defaults: { warning: '>0.7', critical: '>0.9', interval: '1m', periods: 1  },
    type: 'node',
    derivative: false
  },
  'os.load_average.1m': {
    label: 'CPU Load (1m)',
    description: 'The amount of load used for the last 1 minute.',
    format: '0.0%',
    metricAgg: 'avg',
    units: '',
    defaults: { warning: '>2', critical: '>4', interval: '1m', periods: 1 },
    type: 'node',
    derivative: false
  }
};

// Mind the gap... UMD Below
(function(define) {
  define(function (require, exports, module) { return metrics; });
}( // Help Node out by setting up define.
   typeof module === 'object' && typeof define !== 'function' ? function (factory) { module.exports = factory(require, exports, module); } : define
));
