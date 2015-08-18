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
    this.props.changeCluster(this.props.cluster_uuid);
  }

  render() {
    return (
      <tr className={ this.props.status }>
        <td><a onClick={(event) => this.changeCluster(event) }>{ this.props.cluster_uuid }</a></td>
        <td>{ numeral(this.props.stats.node_count).format('0,0') }</td>
        <td>{ numeral(this.props.stats.indice_count).format('0,0') }</td>
        <td>{ formatTime(this.props.stats.uptime) }</td>
        <td>{ numeral(this.props.stats.data).format('0,0[.]0 b') }</td>
        <td className="license">
          <div className="license">{ _.capitalize(this.props.license.type) }</div>
          <div className="expires">Expires { moment(this.props.license.expiry_date_in_millis).format('D MMM YY') }</div>
        </td>
      </tr>
    );
  }

}
module.exports = ClusterRow;
