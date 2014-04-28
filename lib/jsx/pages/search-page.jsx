/** @jsx React.DOM */

module.exports = function (Search, SearchController, FileDropper, React) {
  return React.createClass({
    displayName: 'Search Page',

    getInitialState: function () {
      return {
        searches: SearchController.searches
      };
    },

    handleSearchesUpdate: function (searches) {
      this.setState({searches: searches});
    },

    filesDropped: function (files) {
      files.forEach(SearchController.search);
    },

    removeSearch: function (key) {
      SearchController.remove(key);
    },

    componentDidMount: function () {
      SearchController.addEventListener('searches-updated', this.handleSearchesUpdate);
    },

    componentWillUnmount: function () {
      SearchController.removeEventListener('searches-updated', this.handleSearchesUpdate);
    },

    renderBlank: function () {
      return (
        <FileDropper onFiles={this.filesDropped} className="flex flex-row">
          <div className='center-banner'>Arraste seus vídeos aqui</div>
        </FileDropper>
      );
    },

    renderSearches: function () {
      var _this = this;

      return (
        <FileDropper onFiles={this.filesDropped} className="flex auto-scroll">
          {this.state.searches.map(function (search) {
            return <Search key={search.key} data={search} onClose={_this.removeSearch.bind(_this, search.key)} />
          })}
        </FileDropper>
      );
    },

    render: function () {
      if (this.state.searches.length == 0) {
        return this.renderBlank();
      } else {
        return this.renderSearches();
      }
    }
  });
};
