module.exports = class SearchController
  constructor: (@_, observeit) ->
    observeit().attach(this)

    @searches = []

    setTimeout(=>
      @searches = ['init', 'info', 'upload', 'search', 'download', 'downloaded', 'notfound', 'unchanged', 'uploaded', 'share', 'error']
      @notifySearchesUpdate()
    , 1000)

  notifySearchesUpdate: -> @trigger('searches-updated', @searches)
