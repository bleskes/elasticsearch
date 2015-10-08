define(function (require) {
  /*
   * To use simply
   * new Vector(xValue, yValue)
   *
   * You can add subtract and multiply using provided methods
   *
   * vector1
   *  .add(vector2)
   *  .subtract(vector3)
   *  .multiply(vector4);
   */
  class Vector {
    constructor(x, y) {
      this.x = x || 0;
      this.y = y || 0;
    }
    // If you don't want to modify this object
    clone() { return new Vector(this.x, this.y); }
    toArray() { return [this.x, this.y]; }
    toString() { return this.x + ', ' + this.y; }
    _xPx() { return this.x + 'px'; }
    _yPx() { return this.y + 'px'; }
    add(vector) {
      this.x += vector.x;
      this.y += vector.y;
      return this;
    }
    subtract(vector) {
      this.x = this.x - vector.x;
      this.y = this.y - vector.y;
      return this;
    }
    multiply(vector) {
      this.x *= vector.x;
      this.y *= vector.y;
      return this;
    }
  }

  return Vector;
});
