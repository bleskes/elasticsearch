var metrics = {
  'total.search.query_total': {
    label: 'Search Request Rate',
    description: 'The cluster wide rate at which search reqeusts are being executed.',
    format: '0.00',
    units: 'rps',
    defaults: { warning: '<800', critical: '>1000', interval: 60000 },
    type: 'cluster',
    derivitave: true
  },
  'total.index.query_total': {
    label: 'Indexing Request Rate',
    description: 'The cluster wide rate at which documents are being indexed.',
    format: '0.00',
    units: 'rps',
    defaults: { warning: '<1', critical: '<0.5', interval: 60000 },
    type: 'cluster',
    derivitave: true
  },
  'total.search.query_latency': {
    label: 'Query Latency',
    description: 'The average query latency across the entire cluster.',
    format: '0.00',
    units: 'ms',
    defaults: { warning: '>100', critical: '<200', interval: 60000 },
    type: 'cluster',
    derivitave: true
  },
  'os.cpu.user': {
    label: 'CPU Utilization',
    description: 'The percentage of CPU usage.',
    format: '0.0%',
    units: '',
    defaults: { warning: '>0.7', critical: '>0.9', interval: 60000 },
    type: 'node',
    derivitave: false
  },
  'jvm.mem.heap_used_percent': {
    label: 'JVM Heap Usage',
    description: 'The amound of heap used by the JVM',
    format: '0.0%',
    units: '',
    defaults: { warning: '>0.7', critical: '>0.9', interval: 60000  },
    type: 'node',
    derivitave: false
  },
  'os.load_average.1m': {
    label: 'CPU Load (1m)',
    description: 'The amount of load used for the last 1 minute.',
    format: '0.0%',
    units: '',
    defaults: { warning: '>2', critical: '>4', interval: 60000 },
    type: 'node',
    derivitave: false
  }
};

// Mind the gap... UMD Below
(function(define) {
  define(function (require, exports, module) { return metrics; });
}( // Help Node out by setting up define.
   typeof module === 'object' && typeof define !== 'function' ? function (factory) { module.exports = factory(require, exports, module); } : define
));
