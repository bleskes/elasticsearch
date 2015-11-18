const template = require('plugins/marvel/directives/cluster_status/index.html');
const module = require('ui/modules').get('marvel/directives', []);

module.directive('marvelClusterStatus', (globalState, kbnUrl) => {
  return {
    restrict: 'E',
    template,

    /* The app has the styles of the Bootstrap dropdown component, but not
     * the dropdown JS. So we style the menu as "open" in the markup, and
     * control the actual showing and hiding with this directive. */
    link: (scope) => {
      let isMenuShown = false;

      scope.toggleMenu = () => isMenuShown = !isMenuShown;

      scope.showOrHideMenu = () => isMenuShown;

      scope.changeCluster = (uuid) => {
        if (globalState.cluster !== uuid) {
          globalState.cluster = uuid;
          globalState.save();
          kbnUrl.changePath('/overview');
        } else {
          // clicked on current cluster, just hide the dropdown
          isMenuShown = false;
        }
      };

      scope.createClass = (cluster) => {
        const classes = [cluster.status];
        if (cluster.license.type === 'basic') {
          classes.push('basic');
        }
        return classes.join(' ');
      };

    }
  };
});
