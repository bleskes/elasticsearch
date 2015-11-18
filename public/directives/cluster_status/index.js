define(function (require) {
  var template = require('plugins/marvel/directives/cluster_status/index.html');
  var module = require('ui/modules').get('marvel/directives', []);

  module.directive('marvelClusterStatus', function (globalState, kbnUrl) {
    return {
      restrict: 'E',
      template: template,

      /* The app has the styles of the Bootstrap dropdown component, but not
       * the dropdown JS. So we style the menu as "open" in the markup, and
       * control the actual showing and hiding with this directive. */
      link: function (scope) {
        var isMenuShown = false;

        scope.toggleMenu = function () {
          isMenuShown = !isMenuShown;
        };

        scope.showOrHideMenu = function () {
          return isMenuShown;
        };

        scope.changeCluster = function (uuid) {
          if (globalState.cluster !== uuid) {
            globalState.cluster = uuid;
            globalState.save();
            kbnUrl.changePath('/overview');
          } else {
            // clicked on current cluster, just hide the dropdown
            isMenuShown = false;
          }
        };

      }
    };
  });
});
