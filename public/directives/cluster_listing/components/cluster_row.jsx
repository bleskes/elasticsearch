var React = require('react');
var numeral = require('numeral');
var moment = require('moment');
var _ = require('lodash');

function formatTime(millis) {
  var output = [];
  var duration = moment.duration(millis);
  if (millis < 60000) return `${duration.seconds()}s`;
  if (duration.days()) output.push(`${duration.days()}d`);
  if (duration.hours()) output.push(`${duration.hours()}h`);
  if (duration.minutes()) output.push(`${duration.minutes()}m`);
  return output.join(' ');
}

class ClusterRow extends React.Component {

  changeCluster(event) {
    if (this.props.license.type === 'basic') return;
    this.props.changeCluster(this.props.cluster_uuid);
  }

  render() {

    var self = this;
    function get(path) {
      return _.get(self.props, path);
    }

    var licenseExpiry = (
      <div
        className="expires">
        Expires { moment(get('license.expiry_date_in_millis')).format('D MMM YY') }
        </div>
    );

    if (get('license.expiry_date_in_millis') < moment().valueOf()) {
      licenseExpiry = (<div className="expires expired">Expired</div>);
    }

    var classes = [ get('status') ];
    var notBasic = true;
    if (get('license.type') === 'basic') {
      classes = [ 'basic' ];
      notBasic = false;
    }

    return (
      <tr className={ classes.join(' ') }>
        <td key="Name"><a onClick={(event) => this.changeCluster(event) }>{ get('cluster_name') }</a></td>
        <td key="Nodes">{ notBasic ? numeral(get('stats.nodes.count.total')).format('0,0') : '-' }</td>
        <td key="Indices">{ notBasic ? numeral(get('stats.indices.count')).format('0,0') : '-' }</td>
        <td key="Uptime">{ notBasic ? formatTime(get('stats.nodes.jvm.max_uptime_in_millis')) : '-' }</td>
        <td key="Data">{ notBasic ? numeral(get('stats.indices.store.size_in_bytes')).format('0,0[.]0 b') : '-' }</td>
        <td key="License" className="license">
          <div className="license">{ _.capitalize(get('license.type')) }</div>
          { licenseExpiry }
        </td>
      </tr>
    );
  }

}
module.exports = ClusterRow;
