define(function (require) {

  function MarvelDataSource(index) {
    this.index = index;
    this.data = [];
    this.error = null;
  }

  MarvelDataSource.prototype.register = function (courier) {
    this.searchSource = new courier.SearchSource();
    this.searchSource.set('index', this.index);
    this.initSearchSource();
    this.searchSource.onResults(_.bindKey(this, 'handleResponse'));
    this.searchSource.onError(_.bindKey(this, 'handleError'));
  };

  MarvelDataSource.prototype.initSearchSource = function () {};

  MarvelDataSource.prototype.handleResponse = function (resp) {
    this.data = resp;
  };

  MarvelDataSource.prototype.handleError = function (err) {
    this.error = err;
    this.data = [];
  };

  return MarvelDataSource;


});
