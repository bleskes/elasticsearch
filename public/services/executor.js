const angular = require('angular');
const _ = require('lodash');

const mod = require('ui/modules').get('marvel/executor', []);
mod.service('$executor', (Promise, $timeout, timefilter) => {

  const queue = [];
  let executionTimer;

  /**
   * Resets the timer to start again
   * @returns {void}
   */
  function reset() {
    cancel();
    start();
  }

  /**
   * Cancels the execution timer
   * @returns {void}
   */
  function cancel() {
    if (executionTimer) $timeout.cancel(executionTimer);
  }

  /**
   * Registers a service with the executor
   * @param {object} service The service to register
   * @returns {void}
   */
  function register(service) {
    queue.push(service);
  }

  /**
   * Stops the executor and empties the service queue
   * @returns {void}
   */
  function destroy() {
    cancel();
    queue.splice(0, queue.length);
  }

  /**
   * Runs the queue (all at once)
   * @returns {Promise} a promise of all the services
   */
  function run() {
    return Promise.all(queue.map((service) => {
      return service.execute()
        .then(service.handleResponse || _.noop)
        .catch(service.handleError || _.noop);
    }))
    .finally(reset);
  }

  /**
   * Starts the executor service if the timefilter is not paused
   * @returns {void}
   */
  function start() {
    if (!timefilter.refreshInterval.pause) {
      executionTimer = $timeout(run, timefilter.refreshInterval.value);
    }
  }

  /**
   * Reset the executor when the timefilter updates
   */
  timefilter.on('update', reset);


  /**
   * Expose the methods
   */
  return {
    register,
    start(now = false) {
      if (now) {
        return run();
      }
      start();
    },
    destroy,
    reset,
    cancel
  };
});
