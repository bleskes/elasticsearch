define(function (require) {
  var React = require('react');
  var make = React.DOM;


  function sortManager(cols) {
    var lastSortedColIdx = cols.reduce(function (prev, curr, idx) {
      if (prev !== false) {
        return prev;
      }
      return (curr.sort !== 0 ? idx : null);
    }, false) || 0;

    return function (sortObjIdx) {
      var oldCol = cols[lastSortedColIdx];
      var newCol = cols[sortObjIdx];
      if (sortObjIdx === lastSortedColIdx) {
        oldCol.sort = oldCol.sort === 1 ? -1 : 1;
      } else {
        oldCol.sort = 0;
        newCol.sort = 1;
        lastSortedColIdx = sortObjIdx;
      }
      return cols;
    };
  }

  var TableHead = React.createClass({
    displayName: 'TableHead',
    render: function () {
      var that = this;
      function makeTh(config, idx) {
        var isSortCol = config.sort !== 0 && config.sort;
        var isSortAsc = config.sort === 1;
        var $icon = false;
        if (isSortCol) {
          var iconClassName = 'fa fa-sort-amount-' + (isSortAsc ? 'asc' : 'desc');
          $icon = make.i({className: iconClassName});
        }

        return make.th({
          key: config.title,
          onClick: function () {
            if (config.sort !== 0) {
              config.sort = config.sort === 1 ? -1 : 1;
            } else {
              config.sort = 1;
            }
            that.props.setSortCol(config);
          },
          className: config.className || ''
        }, config.title, $icon);
      }
      var $ths =  this.props.columns.map(makeTh);
      return make.thead(null, make.tr(null, $ths));
    }
  });
  return TableHead;
});
