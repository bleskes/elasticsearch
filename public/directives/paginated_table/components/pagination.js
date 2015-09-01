'use strict';
define(function(require) {
  var React = require('react');
  var make = React.DOM;

  function makeChev(isRight) {
    return make.i({className: 'fa fa-chevron-' + (isRight ? 'right' : 'left')});
  }

  return React.createClass({
    displayName: 'Pagination',
    render: function() {
      var numPages = Math.ceil(this.props.dataLength / this.props.itemsPerPage);
      if (!_.isFinite(numPages)) { numPages = 1; } // Because Dividing by 0 results in Infinity

      var pageList = [];
      if(this.props.pageIdx > 0) {
        pageList.push(make.li({
          'onClick': this.props.setCurrPage.bind(null, this.props.pageIdx-1)
        }, makeChev()));
      }
      // Make the middle pagination
      if (numPages > 1) {
        for (var i = 1; i <= numPages; i++) {
          pageList.push(make.li({
            'key': i,
            'onClick': this.props.setCurrPage.bind(null, i-1),
            'className': (this.props.pageIdx === i-1 ? 'current' : '')
          }, i));
        }
      }
      if(this.props.pageIdx < numPages - 1) {
        pageList.push(make.li({
          'onClick': this.props.setCurrPage.bind(null, this.props.pageIdx+1)
        }, makeChev(true)));
      }
      var $pagination = make.ul({className: 'pagination'}, pageList);

      // Select: 20 | 60 | 80 | ALL
      var showOptions = [];
      var that = this;
      [20, 60, 80, 'Show All'].forEach(function(choice, idx) {
        if (idx) {
          showOptions.push('|');
        }
        showOptions.push(make.span({
          'key': idx,
          'onClick': function () {
            that.props.setItemsPerPage(choice);
          }
        }, ' ' + choice + ' '));
      });
      var $showing = make.div({className: 'pull-right items-per-page'}, 'Show: ', showOptions);
      return make.div({className: 'footer'}, $showing, $pagination);
    }
  });
});
