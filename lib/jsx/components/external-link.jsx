/** @jsx React.DOM */

module.exports = function (React) {
  return React.createClass({
    displayName: 'External Link',

    openExternalLink: function (e) {
      e.preventDefault();

      var url = e.currentTarget.href;

      window.open(url, '_blank');
    },

    render: function () {
      return this.transferPropsTo(
        <a onClick={this.openExternalLink}>{this.props.children}</a>
      );
    }
  });
};
