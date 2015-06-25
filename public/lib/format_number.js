define(['moment', 'numeral'], function(moment, numeral) {
  return function formatNumber(num, which) {
    var format = '0.00';
    var postfix = '';
    switch (which) {
      case 'time_since':
        return moment(moment() - num).from(moment(), true);
      case 'time':
        return moment(num).format('H:mm:ss');
      case 'int_commas':
        format = '0,0'
        break;
      case 'byte':
        format += 'b';
        break;
      case 'ms':
        postfix = 'ms';
        break;
    }
    return numeral(num).format(format) + postfix;
  }
});
