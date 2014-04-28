module.exports = class SearchController
  constructor: (@_, observeit) ->
    observeit().attach(this)

    @keyIndex = 0

    @searches = [{
      key: @keyIndex++
      status: 'init'
      path: 'movie.bla.bla.bla.mkv'
    }]

  notifySearchesUpdate: -> @trigger('searches-updated', @searches)
