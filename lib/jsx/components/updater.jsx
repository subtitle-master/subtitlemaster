/** @jsx React.DOM */

module.exports = function (ExternalLink, sm, React) {
  return React.createClass({
    displayName: 'Updater',

    getInitialState: function () {
      return {
        hasUpdate: false
      };
    },

    componentDidMount: function () {
      var _this = this;

      sm.checkForUpdates().then(function (hasUpdates) {
        _this.setState({hasUpdate: hasUpdates});
      });
    },

    render: function () {
      var classNames = React.addons.classSet({
        notification: true,
        active: this.state.hasUpdate
      });

      return (
        <ExternalLink href="http://www.subtitlemaster.com/" className={classNames}>
          <div>Atualização disponível! Clique aqui para baixar a versão mais recente.</div>
        </ExternalLink>
      );
    }
  });
};
