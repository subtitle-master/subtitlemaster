AppInjector
  .value('nwgui', require('nw.gui'))
  .value('smCore', require('subtitle-master-core'))
  .value('request', require('request'));

AppInjector.call(function (SubtitleMaster, SearchController, React, nwgui) {
  window.React = React;

  React.renderComponent(SubtitleMaster(), document.body);

  nwgui.App.on('open', function (cmd) {
    SearchController.search(cmd);
  });
});
