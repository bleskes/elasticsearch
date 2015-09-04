// Mind the gap... UMD Below
(function (define) {
  define(function (require, exports, module) {
    return function (a, b) {
      if (a.status === b.status) return 0;
      if (a.status === 'red') return -1;
      if (b.status === 'red') return 1;
      if (a.status === 'yellow') return -1;
      if (b.status === 'yellow') return 1;
    };
  });
}(// Help Node out by setting up define.
   typeof module === 'object' &&
   typeof define !== 'function' ? function (factory) { module.exports = factory(require, exports, module); } : define
));

