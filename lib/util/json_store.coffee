module.exports = ->
  class JsonStore
    constructor: (@store) ->

    get: (key) ->
      if @store.hasOwnProperty(key)
        JSON.parse(@store[key])
      else
        null

    set: (key, value) ->
      @store[key] = JSON.stringify(value)

      this
