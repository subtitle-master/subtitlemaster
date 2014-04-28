/** @jsx React.DOM */

module.exports = function (MultiSelect, React) {
  var options = [
    {title: 'hello', value: 'hello'},
    {title: 'world', value: 'world'}
  ];

  return React.createClass({
    displayName: 'Settings Page',

    getInitialState: function () {
      return {
        selected: []
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
