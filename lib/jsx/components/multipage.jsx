/** @jsx React.DOM */

module.exports = function (React) {
  return React.createClass({
    displayName: 'Multi Page',

    propTypes: {
      selected: React.PropTypes.string.isRequired
    },

    render: function () {
      var child = _.find(this.props.children, {props: {page: this.props.selected}});

      if (!child) {
        child = this.props.children[0];
      }

      return this.transferPropsTo(<div>{child}</div>);
    }
  });
};
