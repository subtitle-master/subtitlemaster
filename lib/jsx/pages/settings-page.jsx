/** @jsx React.DOM */

module.exports = function (MultiSelect, React, languages, _) {
  var options = _.map(languages, function (lang) {
    return {title: lang.name, value: lang.iso639_1}
  });

  return React.createClass({
    displayName: 'Settings Page',

    getInitialState: function () {
      return {
        selected: ['pb', 'pt', 'en']
      }
    },

    updateSelected: function (selected) {
      this.setState({selected: selected});
    },

    render: function () {
      return (
        <div className="flex">
          <h1>Preferências</h1>
          <div className="white-box">
            <MultiSelect options={options} selected={this.state.selected} onChange={this.updateSelected} />
          </div>
        </div>
      );
    }
  });
};
