/**
 * ELASTICSEARCH CONFIDENTIAL
 * _____________________________
 *
 *  [2014] Elasticsearch Incorporated All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
 */



define(function (require) {
  var _ = require('lodash');
  var moment = require('moment');
  var module = require('ui/modules').get('marvel/directives', []);

  // Module specific application dependencies
  var getValue = require('plugins/marvel/directives/shard_allocation/lib/getValueFromArrayOrString');
  var labels = require('plugins/marvel/directives/shard_allocation/lib/labels');
  var changeData = require('plugins/marvel/directives/shard_allocation/lib/changeData');
  var updateColors = require('plugins/marvel/directives/shard_allocation/lib/updateColors');
  var extractMarkers = require('plugins/marvel/directives/shard_allocation/lib/extractMarkers');
  var template = require('plugins/marvel/directives/shard_allocation/index.html');

  require('plugins/marvel/directives/shard_allocation/directives/shardGroups');
  require('plugins/marvel/directives/shard_allocation/directives/clusterView');

  module.filter('localTime', function () {
    return function (text) {
      if (!text) {
        return text;
      }
      return moment(text).format('HH:mm:ss.SS');
    };
  });

  module.filter('localDate', function () {
    return function (text) {
      if (!text) {
        return text;
      }
      return moment(text).format('YYYY-MM-DD');
    };
  });

  module.directive('marvelShardAllocation', function (timefilter, globalState, $timeout, Private) {
    var getTimeline = Private(require('plugins/marvel/directives/shard_allocation/requests/getTimelineData'));
    var getStateSource = Private(require('plugins/marvel/directives/shard_allocation/requests/getStateSource'));

    return {
      restrict: 'E',
      template: template,
      scope: {
        indexPattern: '=',
        cluster: '=',
        id: '=',
        view: '@',
        hideUi: '=',
        showHiddenIndices: '=',
        clusterState: '='
      },
      link: function ($scope, el, attr) {
        var handleConnectionError = function () {
          // alertSrv.set('Error',
          //     'The connection to Elasticsearch returned an error. Check your Elasticsearch instance.',
          //     'error', 30000);
        };

        $scope.style = 'light';
        $scope.timelineData = [];
        $scope.showHead = false;

        $scope.player = {
          paused: false,
          fastForward: false,
          forward: false,
          backward: true,
          fastBackward: true
        };

        function getNewTimeRange() {
          var bounds = timefilter.getBounds();
          return {
            gte: bounds.min.valueOf(),
            lte: bounds.max.valueOf(),
            format: 'epoch_millis'
          };
        }

        $scope.timeRange = getNewTimeRange();

        // Defaults for the panel.
        var defaults = {
          show_hidden: ($scope.showHiddenIndices != null) ? $scope.showHiddenIndices : true,
          view: $scope.view || 'node',
          labels: labels.nodes,
          rate: 500,
          showPlayer: true,
          filter: $scope.filterBy
        };

        // Set the defaults for the $scope.panel (this is side effecting)
        $scope.panel = {};
        _.defaults($scope.panel, defaults);

        // Change update the state of the ui based on the view
        $scope.$watch('panel.view', function () {
          changeData($scope);
        });

        // Filter the elements we are showning with the panel.filter
        $scope.filterResults = function () {
          changeData($scope);
        };

        $scope.$watch('panel.filter', _.debounce(function (newValue, oldValue) {
          if (newValue !== oldValue) {
            changeData($scope);
          }
        }, 500));

        // When the panel.show_hidden attribute is set we need to update the state
        // of the ui
        $scope.$watch('panel.show_hidden', function () {
          changeData($scope);
        });


        // This will update the $scope.panel.view variable which will in turn
        // update the ui using the $watch('panel.view') event listener.
        $scope.switchView = function (view) {
          $scope.panel.view = view;
          return false;
        };

        $scope.$watch('player.current', function (newValue) {
          // We can't do anything with an undefined value
          if (_.isUndefined(newValue)) {
            return;
          }

          if (($scope.player.current === $scope.player.total)) {
            $scope.player.forward = false;
            $scope.player.fastForward = false;
            $scope.player.paused = true;
          }
          else {
            $scope.player.forward = true;
            $scope.player.fastForward = true;
          }

          if (($scope.player.current === 0) && ($scope.player.total !== 0)) {
            $scope.player.backward = false;
            $scope.player.fastBackward = false;
          }
          else {
            $scope.player.backward = true;
            $scope.player.fastBackward = true;
          }

          $scope.barX = (($scope.player.current + 1) / $scope.player.total) * 100;
          if ($scope.barX > 100) {
            $scope.barX = 100;
          }

          // Due to the zero based counting and how we track where the head is,
          // when we get to the end we have to subtract one.
          var docIndex = $scope.player.current;
          if ($scope.player.current === $scope.player.total) {
            docIndex--;
          }

          var doc = $scope.timelineData[docIndex];
          var differentState = !$scope.currentState || (doc && doc._id !== $scope.currentState._id);
          if (doc && differentState) {
            getStateSource(doc, $scope.id, $scope.view).then(function (state) {
              $scope.currentState = state;
              changeData($scope);
            }, handleConnectionError);
          }

        });


        var timerId;

        var stop = function () {
          timerId = $timeout.cancel(timerId);
        };

        var changePosition = function () {
          if (!$scope.player.paused && ($scope.player.current !== $scope.player.total)) {
            ++$scope.player.current;
            timerId = $timeout(changePosition, $scope.panel.rate);
          }
        };

        $scope.jump = function ($event) {
          var offsetX = _.isUndefined($event.offsetX) ? $event.originalEvent.layerX : $event.offsetX;
          var position = offsetX / $event.currentTarget.clientWidth;
          $scope.player.current = Math.floor(position * $scope.player.total);
          $scope.player.paused = true;
        };

        $scope.head = function ($event) {
          var offsetX = _.isUndefined($event.offsetX) ? $event.originalEvent.layerX : $event.offsetX;
          var position = offsetX / $event.currentTarget.clientWidth;
          var current = Math.floor(position * $scope.player.total);
          var timestamp = getValue($scope.timelineData[current].fields.timestamp);
          var message = getValue($scope.timelineData[current].fields.message);
          var status = getValue($scope.timelineData[current].fields.status);

          $scope.headX = offsetX;
          $scope.headTime = timestamp;
          $scope.headMessage = message;
          $scope.headStatus = status;
        };

        $scope.$watch('player.paused', function () {
          stop();
          if ($scope.player.paused === false) {
            changePosition();
          }
        });

        $scope.pause = function ($event) {
          $event.preventDefault();
          $scope.player.paused = true;
        };

        $scope.play = function ($event) {
          $event.preventDefault();
          if ($scope.player.current === $scope.player.total) {
            $scope.player.current = 0;
            // We need to put the same amount of delay before we start the animation
            // otherwise it will feel like it's skipping the first frame.
            $timeout(function () {
              $scope.player.paused = false;
            }, $scope.panel.rate);
          }
          else {
            $scope.player.paused = false;
          }
        };

        $scope.forward = function ($event) {
          $event.preventDefault();
          if ($scope.player.current !== $scope.player.total) {
            ++$scope.player.current;
          }
          $scope.player.paused = true;
        };

        $scope.fastForward = function ($event) {
          $event.preventDefault();
          $scope.player.current = $scope.player.total;
          $scope.player.paused = true;
        };

        $scope.backward = function ($event) {
          $event.preventDefault();
          if ($scope.player.current !== 0) {
            --$scope.player.current;
          }
          $scope.player.paused = true;
        };

        $scope.rewind = function ($event) {
          $event.preventDefault();
          $scope.player.current = 0;
          $scope.player.paused = true;
        };

        function clusterStateToDataFormat(state) {
          return {
            _index: state._index,
            _type: state._type,
            _id: state._id,
            fields: {
              'timestamp': [ state.timestamp ],
              'status': [ state.status ],
              'message': [ state.message ]
            }
          };
        }

        var unsubscribe = $scope.$on('updateTimelineData', function (event, direction, newData) {
          var isCurrent = $scope.player.current === $scope.player.total;
          _.each(newData, function (row) {
            if (!_.find($scope.timelineData, { _id: row._id })) {
              $scope.timelineData[direction](row);
            }
          });
          $scope.timelineMarkers = extractMarkers($scope.timelineData);
          $scope.player.total = ($scope.timelineData.length > 0 && $scope.timelineData.length) || 1;
          if (isCurrent) {
            $scope.player.current = $scope.player.total;
            $scope.paused = true;
          }
          updateColors($scope);
          $scope.total = $scope.player.total;
        });
        $scope.$on('$destroy', unsubscribe);

        $scope.$watch('clusterState.data.version', function (version) {
          var current = ($scope.player.current === $scope.player.total);
          var data = $scope.timelineData;
          var last = _.last(data);
          if (last) {
            var newTimestamp = moment($scope.clusterState.data.timestamp).valueOf();
            var lastTimestamp = moment(getValue(last.fields.timestamp)).valueOf();
            var newTimeRange = { gte: lastTimestamp, lte: newTimestamp, format: 'epoch_millis' };
            if (lastTimestamp < newTimestamp) {
              fetch(100, newTimeRange, 'push');
            }
          }
        });

        $scope.$listen(timefilter, 'fetch', function () {
          $scope.timelineData = [];
          $scope.timelineMarkers = [];
          fetch();
          // fetch().then(handleTimeline, handleConnectionError);
        });

        function fetch(size, timeRange, direction) {
          $scope.loading = true;
          direction = direction || 'unshift';
          timeRange = timeRange || getNewTimeRange();
          size = size || 300;
          return getTimeline(direction, $scope.indexPattern, $scope.cluster, size, timeRange).then(function (data) {
            $scope.loading = false;
            return data;
          });
        }

        fetch();
        // fetch().then(handleTimeline, handleConnectionError);

      }
    };

  });
});
