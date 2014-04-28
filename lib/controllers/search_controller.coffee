module.exports = class SearchController
  constructor: (@_, observeit) ->
    observeit().attach(this)

    @searches = []

  notifySearchesUpdate: -> @trigger('searches-updated', @searches)
