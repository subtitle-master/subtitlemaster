AppInjector.call(function (SubtitleMaster, React) {
  window.React = React;

  React.renderComponent(SubtitleMaster(), document.body);
});
