define(function (require) {
  const Vector = require('./vector');
  class Bounds {
    constructor(v1, v2) {
      this.nw = v1 || new Vector();
      this.se = v2 || new Vector();
    }
    contains(vector) {
      return this.nw.x < vector.x &&
        vector.x < this.se.x &&
        this.nw.y < vector.y &&
        vector.y < this.se.y;
    }
  }

  return Bounds;
});
