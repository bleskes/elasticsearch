define(function (require) {
  return function stripEmpties(obj) {
    for (var i in obj) {
      if (obj[i] == null || (typeof obj[i] === 'string' && !obj[i])) {
        delete obj[i];
      }
      if (typeof obj[i] === 'object') {
        stripEmpties(obj[i]);
      }
    }
    return obj;
  };
});
