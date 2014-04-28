/** @jsx React.DOM */

module.exports = function (React) {
  window.ondragover = function (e) {
    e.preventDefault();

    return false;
  };

  window.ondrop = function (e) {
    e.preventDefault();

    return false;
  };

  return React.createClass({
    displayName: 'File Dropper',

    propTypes: {
      onFiles: React.PropTypes.func
    },

    getDefaultProps: function () {
      return {
        onFiles: function () { }
      }
    },

    getInitialState: function () {
      return {
        over: false
      };
    },

    dragover: function () {
      this.setState({over: true});

      return false;
    },

    dragleave: function () {
      this.setState({over: false});

      return false;
    },

    drop: function (e) {
      e.preventDefault();

      this.dragleave();
      this.props.onFiles(_.map(e.dataTransfer.files, function (file) {
        return file.path || file.name;
      }));

      return false;
    },

    render: function () {
      var className = React.addons.classSet({
        dragging: this.state.over
      });

      return this.transferPropsTo(
        <div className={className}
             onDragEnter={this.dragover}
             onDragOver={this.dragover}
             onDragLeave={this.dragleave}
             onDragEnd={this.dragleave}
             onDrop={this.drop}>
          {this.props.children}
        </div>
      );
    }
  });
};
