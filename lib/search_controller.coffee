module.exports = class SearchController
  constructor: (@_) ->
    @listeners = {}
    @searches = []

    setTimeout(=>
      @searches = ['init', 'info', 'upload', 'search', 'download', 'downloaded', 'notfound', 'unchanged', 'uploaded', 'share', 'error']
      @notifySearchesUpdate()
    , 1000)

  addEventListener: (event, listener) ->
    @listeners[event] ||= []
    @listeners[event].push(listener)

    this

  removeEventListener: (event, listener) ->
    @listeners[event] = @_.without(@listeners[event] || [], listener)

    this

  trigger: (event, args...) ->
    listener(args...) for listener in (@listeners[event] || [])

    this

  notifySearchesUpdate: -> @trigger('searches-updated', @searches)
