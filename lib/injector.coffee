injector = require('inject-it')()

# third part libraries
injector.value('React', require('react/addons'))
injector.value('_', require('lodash'))
injector.value('$', require('jquery'))
injector.value('observeit', require('observe-it'))
injector.value('W', require('when'))

# local libraries
injector.factory('arrayMove', require('./util/array_move.coffee'))

# controllers
injector.service('SearchController', require('./controllers/search_controller.coffee'))

# components
injector.factory('Search', require('./jsx/components/search.jsx'))
injector.factory('ExternalLink', require('./jsx/components/external-link.jsx'))
injector.factory('MultiPage', require('./jsx/components/multipage.jsx'))
injector.factory('MultiSelect', require('./jsx/components/multiselect.jsx'))

# pages
injector.factory('SearchPage', require('./jsx/pages/search-page.jsx'))
injector.factory('SettingsPage', require('./jsx/pages/settings-page.jsx'))

# app root
injector.factory('SubtitleMaster', require('./jsx/subtitle-master.jsx'))

# these are gateways to stuff that can't be used on browser direct, so use fakes for UI testing
injector.factory('gui', require('./browser_fakes/gui.coffee'))
injector.factory('sm', require('./browser_fakes/sm.coffee'))

# others
injector.value('languages', require('subtitle-master-core/lib/languages.coffee'))

module.exports = injector
