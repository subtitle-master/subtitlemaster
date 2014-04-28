module.exports = class SettingsController
  constructor: (localStorage, JsonStore) ->
    @prefix = 'settings-'

    @defaults =
      languages: ['pb', 'pt', 'en']

    @store = new JsonStore(localStorage)

  get: (key) => @store.get(@prefix + key) || @defaults[key]
  set: (key, value) => @store.set(@prefix + key, value)
