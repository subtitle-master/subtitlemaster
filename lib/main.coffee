injector = require('./injector.coffee')

injector.factory('gui', require('./integration/browser/gui.coffee'))
injector.factory('sm', require('./integration/browser/sm.coffee'))
injector.factory('track', require('./integration/browser/track.coffee'))

window.AppInjector = injector
