injector = require('./injector.coffee')

injector.factory('gui', require('./integration/node_webkit/gui.coffee'))
injector.factory('sm', require('./integration/node_webkit/sm.coffee'))
injector.factory('track', require('./integration/node_webkit/track.coffee'))

window.AppInjector = injector
