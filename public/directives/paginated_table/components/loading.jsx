var React = require('react');
class Loading extends React.Component {
  render() {
    return (
      <tbody>
        <tr>
          <td>
            <i className="fa fa-spinner fa-pulse"></i>
            <span>Loading data...</span>
          </td>
        </tr>
      </tbody>
      );
  }
}
module.exports = Loading;
