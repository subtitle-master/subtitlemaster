/** @jsx React.DOM */

module.exports = function (React, gui) {
  return React.createClass({
    displayName: 'External Link',

    openExternalLink: function (e) {
      e.preventDefault();

      var url = e.currentTarget.href;

      gui.openExternalUrl(url);
    },

    render: function () {
      return this.transferPropsTo(
        <a onClick={this.openExternalLink}>{this.props.children}</a>
      );
    }
  });
};
