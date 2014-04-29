/** @jsx React.DOM */

module.exports = function (track, SearchPage, SettingsPage, ExternalLink, MultiPage, React) {
  track.screen('search');

  return React.createClass({
    displayName: 'Subtitle Master',

    getInitialState: function () {
      return {
        selectedPage: 'search'
      }
    },

    changePage: function (page, e) {
      e.preventDefault();

      if (page == this.state.selectedPage) return;

      track.screen(page);
      this.setState({selectedPage: page});
    },

    navigateButton: function (page, childs) {
      var classes = React.addons.classSet({
        button: true,
        selected: page == this.state.selectedPage
      });

      return <a className={classes} href="#" data-page={page} onClick={this.changePage.bind(this, page)}>{childs}</a>
    },

    render: function () {
      return (
        <div className="app-container flex-column">
          <hr className="filmstrip shadow-down" />
          <MultiPage className="flex flex-row auto-scroll" selected={this.state.selectedPage}>
            <SearchPage page="search" />
            <SettingsPage page="settings" />
          </MultiPage>
          <hr className="filmstrip shadow-up" />
          <div className="app-menu flex-row">
            {this.navigateButton('search', <img src="images/icons/magnify.png" />)}
            <div className="flex" />
            <ExternalLink className="button" href="https://www.facebook.com/subtitlemaster"><img src="images/icons/facebook.png" /></ExternalLink>
            {this.navigateButton('settings', <img src="images/icons/gear.png" />)}
          </div>
        </div>
      );
    }
  });
};
