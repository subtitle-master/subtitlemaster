// override fakes with real dependencies
AppInjector
  .value('nwgui', require('nw.gui'));

AppInjector.call(function (SubtitleMaster, React) {
  window.React = React;

  React.renderComponent(SubtitleMaster(), document.body);
});
