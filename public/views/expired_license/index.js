define(function (require) {
  require('angular');

  require('ui/routes').when('/expired-license', {
    template: require('plugins/marvel/views/expired_license/index.html')
  });

  const module = require('ui/modules').get('marvel', [
    'marvel/directives'
  ]);
});
