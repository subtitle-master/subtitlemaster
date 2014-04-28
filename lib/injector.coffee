injector = require('inject-it')()

injector.value('React', require('react/addons'))
injector.value('_', require('lodash'))
injector.value('$', require('jquery'))
injector.value('observeit', require('observe-it'))

injector.factory('arrayMove', require('./array_move.coffee'))

injector.service('SearchController', require('./search_controller.coffee'))

injector.factory('Search', require('./jsx/components/search.jsx'))
injector.factory('ExternalLink', require('./jsx/components/external-link.jsx'))
injector.factory('MultiPage', require('./jsx/components/multipage.jsx'))
injector.factory('MultiSelect', require('./jsx/components/multiselect.jsx'))

injector.factory('SearchPage', require('./jsx/pages/search-page.jsx'))
injector.factory('SettingsPage', require('./jsx/pages/settings-page.jsx'))

injector.factory('SubtitleMaster', require('./jsx/subtitle-master.jsx'))

module.exports = injector
