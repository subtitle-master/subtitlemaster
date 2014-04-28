module.exports = (smCore, W, localStorage) ->
  key = (hash) -> "upload-cache-#{hash}"

  localCache =
    check: (hash) -> W localStorage[key(hash)]
    put: (hash) -> W localStorage[key(hash)] = true

  search: (path) ->
    console.log 'creating search on webkit', path, smCore
    operation = new smCore.SearchDownload(path, ['pb', 'en'], localCache)
    operation.run()
