injector = require('./injector.coffee')

injector.factory('gui', require('./integration/browser/gui.coffee'))
injector.factory('sm', require('./integration/browser/sm.coffee'))

window.AppInjector = injector
