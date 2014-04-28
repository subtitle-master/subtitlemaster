/** @jsx React.DOM */

module.exports = function (Search, SearchController, React) {
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

    componentDidMount: function () {
      SearchController.addEventListener('searches-updated', this.handleSearchesUpdate);
    },

    componentWillUnmount: function () {
      SearchController.removeEventListener('searches-updated', this.handleSearchesUpdate);
    },

    renderBlank: function () {
      return (
        <div className="flex flex-row">
          <div className='center-banner'>Arraste seus vídeos aqui</div>
        </div>
      );
    },

    renderSearches: function () {
      return (
        <div className="flex">
          {this.state.searches.map(function (search) {
            return <Search key={search.key} data={search} />
          })}
        </div>
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
