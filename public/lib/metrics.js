define(function (require) {
  return {
    'os.cpu.user': { label: 'CPU Utilization', format: '0.0%', units: '', warning: 0.7, critical: 0.9, interval: 60000, type: 'node' }
  };
});
