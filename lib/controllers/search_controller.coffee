flows = ['unchanged', 'upload', 'uploadDownload', 'download', 'missing', 'error']

module.exports = class SearchController
  constructor: (@_, observeit, @sm) ->
    observeit().attach(this)

    @keyIndex = 0
    @searches = []

  search: (path) =>
    @sm.scanPath(path).then undefined, ((err) -> console.log('error', err)), ({value}) =>
      @searchPath(value)

  searchPath: (path) =>
    searchObject = {key: @keyIndex++, path, status: 'init'}

    @searches.push(searchObject)

    @sm.search(path).then(
      (status) => searchObject.status = status; @notifySearchesUpdate()
      (err) => searchObject.status = 'error'; searchObject.error = err; @notifySearchesUpdate();
      ([status, info]) => searchObject.status = status; searchObject[status] = info; @notifySearchesUpdate()
    )

  remove: (key) =>
    @searches = @_.reject(@searches, {key})
    @notifySearchesUpdate()

  clear: =>
    @searches = []
    @notifySearchesUpdate()

  notifySearchesUpdate: -> @trigger('searches-updated', @searches)

  # this code is for development UI testing only

  flowBomb: (n) =>
    for i in [0..n]
      setTimeout =>
        @search(_.sample(flows))
      , _.random(0, 400)

    this
