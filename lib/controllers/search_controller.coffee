flows = ['unchanged', 'upload', 'uploadDownload', 'download', 'missing', 'error']

module.exports = class SearchController
  constructor: (@_, observeit, @sm) ->
    observeit().attach(this)

    @keyIndex = 0
    @searches = []

  search: (path) =>
    searchObject = {key: @keyIndex++, path, status: 'init'}

    @searches.push(searchObject)

    @sm.search(path).then(
      (status) => searchObject.status = status; @notifySearchesUpdate()
      (err) => searchObject.status = 'error'; searchObject.error = err; @notifySearchesUpdate(); console.log(err)
      ([status, info]) => searchObject.status = status; searchObject[status] = info; @notifySearchesUpdate()
    )

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
