injector = require('./injector.coffee')

injector.call (SubtitleMaster, React) ->
  window.React = React
  window.injector = injector

  React.renderComponent(SubtitleMaster(), document.body)
