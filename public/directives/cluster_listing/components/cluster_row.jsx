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
    if (this.props.license.type === 'lite') return;
    this.props.changeCluster(this.props.cluster_uuid);
  }

  render() {

    var licenseExpiry = (
      <div
        className="expires">
        Expires { moment(this.props.license.expiry_date_in_millis).format('D MMM YY') }
        </div>
    );

    if (this.props.license.expiry_date_in_millis < moment().valueOf()) {
      licenseExpiry = (<div className="expires expired">Expired</div>);
    }

    var classes = [ this.props.status ];
    var notLite = true;
    if (this.props.license.type === 'lite') {
      classes = [ 'lite' ];
      notLite = false;
    }

    return (
      <tr className={ classes.join(' ') }>
        <td><a onClick={(event) => this.changeCluster(event) }>{ this.props.cluster_name }</a></td>
        <td>{ notLite ? numeral(this.props.stats.node_count).format('0,0') : '-' }</td>
        <td>{ notLite ? numeral(this.props.stats.indice_count).format('0,0') : '-' }</td>
        <td>{ notLite ? formatTime(this.props.stats.uptime) : '-' }</td>
        <td>{ notLite ? numeral(this.props.stats.data).format('0,0[.]0 b') : '-' }</td>
        <td className="license">
          <div className="license">{ _.capitalize(this.props.license.type) }</div>
          { licenseExpiry }
        </td>
      </tr>
    );
  }

}
module.exports = ClusterRow;
