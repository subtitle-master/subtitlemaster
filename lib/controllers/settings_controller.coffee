module.exports = class SettingsController
  constructor: (localStorage, JsonStore, uuid) ->
    @prefix = 'settings-'

    @defaults =
      languages: ['pb', 'pt', 'en']

    @store = new JsonStore(localStorage)

    @set('uuid', uuid.v4()) unless @get('uuid')

  get: (key) => @store.get(@prefix + key) || @defaults[key]
  set: (key, value) => @store.set(@prefix + key, value)
