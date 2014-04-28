module.exports = (smCore, W, localStorage, SettingsController) ->
  key = (hash) -> "upload-cache-#{hash}"

  localCache =
    check: (hash) -> W localStorage[key(hash)]
    put: (hash) -> W localStorage[key(hash)] = true

  search: (path) ->
    operation = new smCore.SearchDownload(path, SettingsController.get('languages'), localCache)
    operation.run()

  scanPath: (path) -> smCore.VideoScan([path])
