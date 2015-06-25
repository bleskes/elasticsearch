var metrics = {
  'search_request_rate': {
    field: 'total.search.query_total',
    label: 'Search Request Rate',
    description: 'The cluster wide rate at which search reqeusts are being executed.',
    format: '0.00',
    metricAgg: 'max',
    units: 'rps',
    defaults: { warning: '>100', critical: '>5000', interval: '1m', periods: 1 },
    type: 'cluster',
    derivative: true
  },
  'index_request_rate': {
    field: 'primaries.indexing.index_total',
    label: 'Indexing Request Rate',
    description: 'The cluster wide rate at which documents are being indexed.',
    format: '0.00',
    metricAgg: 'max',
    units: 'rps',
    defaults: { warning: '>1000', critical: '>5000', interval: '1m', periods: 1 },
    type: 'cluster',
    derivative: true
  },
  'query_latency': {
    field: 'total.search.query_latency',
    label: 'Query Latency',
    description: 'The average query latency across the entire cluster.',
    format: '0.00',
    metricAgg: 'sum',
    aggs: {
      query_time_in_millis: {
        max: { field: 'total.search.query_time_in_millis' }
      },
      query_total: {
        max: { field: 'total.search.query_total' }
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
    derivitave: false,
    calculation: function (last) {
      var required = last &&
          last.query_time_in_millis_deriv &&
          last.query_total_deriv &&
          last.query_total_deriv.value &&
          last.query_time_in_millis_deriv.value;
      if (required) {
        return last.query_time_in_millis_deriv.value / last.query_total_deriv.value;
      }
    }
  },
  'cpu_utilization': {
    field: 'os.cpu.user',
    label: 'CPU Utilization',
    description: 'The percentage of CPU usage.',
    format: '0.0%',
    metricAgg: 'avg',
    units: '',
    defaults: { warning: '>0.7', critical: '>0.9', interval: '1m', periods: 1 },
    type: 'node',
    derivative: false
  },
  'heap_used_percent': {
    field: 'jvm.mem.heap_used_percent',
    label: 'JVM Heap Usage',
    description: 'The amound of heap used by the JVM',
    format: '0.0%',
    metricAgg: 'avg',
    units: '',
    defaults: { warning: '>0.7', critical: '>0.9', interval: '1m', periods: 1  },
    type: 'node',
    derivative: false
  },
  'load_average_1m': {
    field: 'os.load_average.1m',
    label: 'CPU Load (1m)',
    description: 'The amount of load used for the last 1 minute.',
    format: '0.0%',
    metricAgg: 'avg',
    units: '',
    defaults: { warning: '>2', critical: '>4', interval: '1m', periods: 1 },
    type: 'node',
    derivative: false
  },
  'index_throttle_time': {
    field: 'primaries.indexing.throttle_time_in_millis',
    label: 'Indexing Throttle Time',
    description: 'The amount of load used for the last 1 minute.',
    format: '0.0',
    metricAgg: 'max',
    units: 'ms',
    defaults: { warning: '>0', critical: '>0', interval: '1m', periods: 1 },
    type: 'index',
    derivative: true
  }
};

// Mind the gap... UMD Below
(function(define) {
  define(function (require, exports, module) { return metrics; });
}( // Help Node out by setting up define.
   typeof module === 'object' && typeof define !== 'function' ? function (factory) { module.exports = factory(require, exports, module); } : define
));
