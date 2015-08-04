var metrics = {
  'search_request_rate': {
    active: true,
    field: 'total.search.query_total',
    label: 'Search Rate',
    description: 'The cluster wide rate at which search reqeusts are being executed.',
    format: '0,0.0',
    metricAgg: 'max',
    units: '/s',
    defaults: { warning: '>100', critical: '>5000', interval: '1m', periods: 1 },
    type: 'cluster',
    filters: [ { term: { _type: 'indices_stats' } } ],
    derivative: true
  },
  'index_request_rate': {
    active: true,
    field: 'total.indexing.index_total',
    label: 'Indexing Rate',
    description: 'The cluster wide rate at which documents are being indexed.',
    format: '0,0.0',
    metricAgg: 'max',
    units: '/s',
    defaults: { warning: '>1000', critical: '>5000', interval: '1m', periods: 1 },
    type: 'cluster',
    filters: [ { term: { _type: 'indices_stats' } } ],
    derivative: true
  },
  'index_latency': {
    active: true,
    field: 'total.indexing.index_total',
    label: 'Indexing Latency',
    description: 'The average indexing latency across the entire cluster.',
    format: '0,0.0',
    metricAgg: 'sum',
    aggs: {
      index_time_in_millis: {
        max: { field: 'total.indexing.index_time_in_millis' }
      },
      index_total: {
        max: { field: 'total.indexing.index_total' }
      },
      index_time_in_millis_deriv: {
        derivative: { buckets_path: 'index_time_in_millis', gap_policy: 'insert_zeros' }
      },
      index_total_deriv: {
        derivative: { buckets_path: 'index_total', gap_policy: 'insert_zeros' }
      }
    },
    units: 'ms',
    defaults: { warning: '>100', critical: '>200', interval: '1m', periods: 1 },
    filters: [ { term: { _type: 'indices_stats' } } ],
    type: 'cluster',
    derivitave: false,
    calculation: function (last) {
      var required = last &&
          last.index_time_in_millis_deriv &&
          last.index_total_deriv &&
          last.index_total_deriv.value &&
          last.index_time_in_millis_deriv.value;
      if (required) {
        return last.index_time_in_millis_deriv.value / last.index_total_deriv.value;
      }

      return 0;
    }
  },
  'query_latency': {
    active: true,
    field: 'total.search.query_total',
    label: 'Query Latency',
    description: 'The average query latency across the entire cluster.',
    format: '0,0.0',
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
    filters: [ { term: { _type: 'indices_stats' } } ],
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

      return 0;
    }
  },
  'cpu_utilization': {
    active: true,
    field: 'os.cpu.user',
    label: 'CPU Utilization',
    description: 'The percentage of CPU usage.',
    format: '0,0.0',
    metricAgg: 'avg',
    units: '%',
    defaults: { warning: '>70', critical: '>90', interval: '1m', periods: 1 },
    type: 'node',
    derivative: false
  },
  'heap_used_percent': {
    active: true,
    field: 'jvm.mem.heap_used_percent',
    label: 'JVM Heap Usage',
    description: 'The amound of heap used by the JVM',
    format: '0,0.0',
    metricAgg: 'avg',
    units: '%',
    defaults: { warning: '>7', critical: '>9', interval: '1m', periods: 1  },
    type: 'node',
    derivative: false
  },
  'load_average_1m': {
    active: true,
    field: 'os.load_average.1m',
    label: 'CPU Load (1m)',
    description: 'The amount of load used for the last 1 minute.',
    format: '0,0.0',
    metricAgg: 'avg',
    units: '',
    defaults: { warning: '>2', critical: '>4', interval: '1m', periods: 1 },
    type: 'node',
    derivative: false
  },
  'index_throttle_time': {
    active: true,
    field: 'primaries.indexing.throttle_time_in_millis',
    label: 'Indexing Throttle Time',
    description: 'The amount of load used for the last 1 minute.',
    format: '0,0.0',
    metricAgg: 'max',
    units: 'ms',
    defaults: { warning: '>0', critical: '>0', interval: '1m', periods: 1 },
    type: 'index',
    derivative: true
  },
  'index_shard_query_rate': {
    active: false,
    field: 'total.search.query_total',
    label: 'Index Search Query Shard Rate',
    description: 'Total number of requests (GET /_search)across an index (and across all relevant shards for that index) / <time range>',
    format: '0.0',
    metricAgg: 'max',
    units: '',
    defaults: { warning: '>0', critical: '>0', interval: '1m', periods: 1 },
    type: 'index',
    derivative: true
  },
  'index_document_count': {
    active: false,
    field: 'primaries.docs.count',
    label: 'Indexing Document Count',
    description: 'Total number of documents (in primary shards) for an index',
    format: '0,0',
    metricAgg: 'max',
    units: '',
    defaults: { warning: '>0', critical: '>0', interval: '1m', periods: 1 },
    type: 'index',
    derivative: false
  },
  'index_search_request_rate': {
    active: true,
    field: 'total.search.query_total',
    label: 'Search Rate',
    description: 'The per index rate at which search reqeusts are being executed.',
    format: '0,0.0',
    metricAgg: 'max',
    units: '/s',
    defaults: { warning: '>100', critical: '>5000', interval: '1m', periods: 1 },
    type: 'index',
    derivative: true
  },
  'index_request_rate': {
    active: true,
    field: 'total.indexing.index_total',
    label: 'Indexing Rate',
    description: 'The per index rate at which documents are being indexed.',
    format: '0,0.0',
    metricAgg: 'max',
    units: '/s',
    defaults: { warning: '>1000', critical: '>5000', interval: '1m', periods: 1 },
    type: 'index',
    derivative: true
  },
  'index_merge_rate': {
    active: true,
    field: 'total.merges.total_size_in_bytes',
    label: 'Indexing Rate',
    description: 'The per index rate at which segements are being merged.',
    format: '0,0.0',
    metricAgg: 'max',
    units: '/s',
    defaults: { warning: '>1000', critical: '>5000', interval: '1m', periods: 1 },
    type: 'index',
    derivative: true
  },
  'index_size': {
    active: true,
    field: 'total.store.size_in_bytes',
    label: 'Index Size',
    description: 'The size of the index.',
    format: '0,0.0b',
    metricAgg: 'max',
    units: '',
    defaults: { warning: '>1000', critical: '>5000', interval: '1m', periods: 1 },
    type: 'index',
    derivative: false
  },
  'index_lucene_memory': {
    active: true,
    field: 'total.segments.memory_in_bytes',
    label: 'Lucene Memory',
    description: 'The amount of memory used by Lucene.',
    format: '0,0.0b',
    metricAgg: 'max',
    units: '',
    defaults: { warning: '>1000', critical: '>5000', interval: '1m', periods: 1 },
    type: 'index',
    derivative: false
  },
  'index_refresh_time': {
    active: true,
    field: 'total.refresh.total_time_in_millis',
    label: 'Total Refresh Time',
    description: 'The the amount of time a refresh takes',
    format: '0,0.0',
    metricAgg: 'max',
    units: '',
    defaults: { warning: '>1000', critical: '>5000', interval: '1m', periods: 1 },
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
