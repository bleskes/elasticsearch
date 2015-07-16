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
  var module = require('modules').get('marvel/directives', []);

  // Module specific application dependencies
  var getValue = require('marvel/directives/shard_allocation/lib/getValueFromArrayOrString');
  var labels = require('marvel/directives/shard_allocation/lib/labels');
  var changeData = require('marvel/directives/shard_allocation/lib/changeData');
  var updateColors = require('marvel/directives/shard_allocation/lib/updateColors');
  var extractMarkers = require('marvel/directives/shard_allocation/lib/extractMarkers');
  var template = require('text!marvel/directives/shard_allocation/index.html');

  require('marvel/directives/shard_allocation/directives/shardGroups');
  require('marvel/directives/shard_allocation/directives/segments');
  require('marvel/directives/shard_allocation/directives/clusterView');

  module.filter('localTime', function () {
    return function (text) {
      if (!text) {
        return text;
      }
      return moment(text).format('YYYY-MM-DD HH:mm:ss.SS');
    };
  });

  function Test(argument) {
  }


  module.directive('marvelShardAllocation', function (timefilter, $timeout, Private) {
    var getTimeline = Private(require('marvel/directives/shard_allocation/requests/getTimelineData'));
    var getStateSource = Private(require('marvel/directives/shard_allocation/requests/getStateSource'));

    return {
      restrict: 'E',
      template: template,
      scope: {
        source: '='
      },
      link: function ($scope, el, attr) {
        var handleConnectionError = function () {
          alertSrv.set('Error',
              'The connection to Elasticsearch returned an error. Check your Elasticsearch instance.',
              'error', 30000);
        };

        $scope.style = dashboard.current.style;
        $scope.timelineData = [];
        $scope.showHead = false;
        $scope.startup = true;

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
          show_hidden: false,
          view: 'nodes',
          labels: labels.nodes,
          rate: 500,
          showPlayer: true
        };

        // Set the defaults for the $scope.panel (this is side effecting)
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
          if (doc) {
            getStateSource(doc).then(function (state) {
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
          var timestamp = getValue($scope.timelineData[current].fields['@timestamp']);
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
              '@timestamp': [ state['@timestamp'] ],
              'status': [ state.status ],
              'message': [ state.message ]
            }
          };
        }

        var handleTimeline = function (data, resetCurrentAndPaused) {
          // If we get nothing back we need to use the current state.
          resetCurrentAndPaused = _.isUndefined(resetCurrentAndPaused) ? true : resetCurrentAndPaused;

          if (data.length === 0) {
            data = [clusterStateToDataFormat($clusterState.state)];
          }

          $scope.timelineData = data;
          $scope.timelineMarkers = extractMarkers(data);
          $scope.player.total = (data.length > 0 && data.length) || 1;
          if (resetCurrentAndPaused) {
            $scope.player.current = $scope.player.total;
            $scope.paused = true;
          }
          updateColors($scope);
          $scope.total = $scope.player.total;
        };

        var unsubscribe = $scope.$on('courier:searchRefresh', function () {

          var timeRange = getNewTimeRange();
          var timeChanged = (timeRange.gte !== $scope.timeRange.gte ||
              timeRange.lte !== $scope.timeRange.lte);

          if (timeChanged) {
            $scope.timeRange = timeRange;
            getTimeline().then(handleTimeline, handleConnectionError).then(function () {
              // Don't start listening to updates till we finish initlaizing
              if ($scope.startup) {
                $scope.startup = false;
              }
            });
          }

        });
        $scope.$on('$destroy', unsubscribe);

      }
    };

  });
});
