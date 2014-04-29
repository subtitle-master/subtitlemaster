// these depenencies can't be inject into the webpack process, they need to be injected
// on the node-webkit environment, that's why they happen in a separated file
AppInjector
  .value('nwgui', require('nw.gui'))
  .value('smCore', require('subtitle-master-core'))
  .value('request', require('request'))
  .value('os', require('os'));

AppInjector.call(function (SubtitleMaster, SearchController, React, nwgui) {
  window.React = React;

  React.renderComponent(SubtitleMaster(), document.body);

  nwgui.App.on('open', function (cmd) {
    SearchController.search(cmd);
  });
});
