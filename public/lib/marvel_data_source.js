define(function (require) {

  function MarvelDataSource(index, cluster) {
    this.index = index;
    this.cluster = cluster;
    this.data = [];
    this.error = null;
  }

  MarvelDataSource.prototype.register = function (courier) {
    this.searchSource = new courier.SearchSource();
    this.searchSource.set('index', this.index);
    this.initSearchSource();
    this.searchSource.set('filter', _.bindKey(this, 'getFilters'));
    this.searchSource.onResults(_.bindKey(this, 'handleResponse'));
    this.searchSource.onError(_.bindKey(this, 'handleError'));
  };

  MarvelDataSource.prototype.initSearchSource = function () {};

  MarvelDataSource.prototype.getFilters = function () {
    var id = (_.isObject(this.cluster)) ? this.cluster._id : this.cluster;
    return [{ term: { 'cluster_name.raw': id } }];
  };

  MarvelDataSource.prototype.handleResponse = function (resp) {
    this.data = resp;
  };

  MarvelDataSource.prototype.handleError = function (err) {
    this.error = err;
    this.data = [];
  };

  MarvelDataSource.prototype.destroy = function () {
    this.searchSource.destroy();
  };

  return MarvelDataSource;


});
