var React = require('react');
class Loading extends React.Component {
  render() {
    return (
      <tbody>
        <tr>
          <td>
            <span>There are no records that match your query.</span>
          </td>
        </tr>
      </tbody>
      );
  }
}
module.exports = Loading;

