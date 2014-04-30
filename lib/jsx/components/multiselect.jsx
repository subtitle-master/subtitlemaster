/** @jsx React.DOM */

module.exports = function (React, _, $, arrayMove) {
  return React.createClass({
    displayName: 'Multiselect',

    propTypes: {
      onChange: React.PropTypes.func,
      options: React.PropTypes.array.isRequired,
      selected: React.PropTypes.array
    },

    getDefaultProps: function () {
      return {
        onChange: function () { },
        selected: []
      };
    },

    getOptions: function () {
      var available = _.clone(this.props.options);
      var selected = [];

      this.props.selected.forEach(function (value) {
        var option = _.find(available, {value: value});

        if (option) {
          available = _.without(available, option);
          selected.push(option);
        }
      });

      return {
        available: available,
        selected: selected
      };
    },

    renderOptions: function (options) {
      return options.map(function (option) {
        return <option key={option.value} value={option.value}>{option.title}</option>
      });
    },

    addItems: function () {
      var newValues = $(this.refs.available.getDOMNode()).val();

      if (!newValues) return;

      this.props.onChange(this.props.selected.concat(newValues));
    },

    removeItems: function () {
      var toRemove = $(this.refs.selected.getDOMNode()).val();

      if (!toRemove) return;

      this.props.onChange(_.difference(this.props.selected, toRemove));
    },

    moveUp: function () {
      var values = $(this.refs.selected.getDOMNode()).val();

      if (!values) return;

      var selected = _.clone(this.props.selected);

      arrayMove.up(selected, values);
      this.props.onChange(selected);
    },

    moveDown: function () {
      var values = $(this.refs.selected.getDOMNode()).val();

      if (!values) return;

      var selected = _.clone(this.props.selected);

      arrayMove.down(selected, values);
      this.props.onChange(selected);
    },

    render: function () {
      var options = this.getOptions();

      return (
        <table className="multiselect">
          <tr>
            <td></td>
            <td>Idiomas Selecionados</td>
            <td></td>
            <td>Idiomas Disponíveis</td>
          </tr>
          <tr>
            <td>
              <div className="flex-column flex-center">
                <button type="button" className="multiselect-moveup" onClick={this.moveUp}>&uarr;</button>
                <button type="button" className="multiselect-movedown" onClick={this.moveDown}>&darr;</button>
              </div>
            </td>
            <td>
              <select size="5" multiple="multiple" ref="selected" className="multiselect-selected">
                {this.renderOptions(options.selected)}
              </select>
            </td>
            <td>
              <div className="flex-column flex-center">
                <button type="button" className="multiselect-add" onClick={this.addItems}>&larr;</button>
                <button type="button" className="multiselect-remove" onClick={this.removeItems}>&rarr;</button>
              </div>
            </td>
            <td>
              <select id="selected" size="5" multiple="multiple" ref="available" className="multiselect-available">
                {this.renderOptions(options.available)}
              </select>
            </td>
          </tr>
        </table>
      );
    }
  });
};
