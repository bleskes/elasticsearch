var React = require('react');
class Loading extends React.Component {
  render() {
    const colSpan = this.props.columns.length;
    return (
      <tbody>
        <tr>
          <td colSpan={ colSpan } calssName="loading">
            <i className="fa fa-spinner fa-pulse"></i>
            <span>Loading data...</span>
          </td>
        </tr>
      </tbody>
      );
  }
}
module.exports = Loading;
