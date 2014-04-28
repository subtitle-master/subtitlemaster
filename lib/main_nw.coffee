injector = require('./injector.coffee')

injector.factory('gui', require('./integration/node_webkit/gui.coffee'))
injector.factory('sm', require('./integration/node_webkit/sm.coffee'))

window.AppInjector = injector
