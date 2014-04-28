AppInjector
  .value('nwgui', require('nw.gui'))
  .value('smCore', require('subtitle-master-core'));

AppInjector.call(function (SubtitleMaster, React, nwgui) {
  window.React = React;

  React.renderComponent(SubtitleMaster(), document.body);

  nwgui.App.on('open', function (cmd) {
    console.log('received', cmd);
  });
});
