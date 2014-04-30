module.exports = (smCore, W, localStorage, SettingsController, request, nodefn, semver, pkg) ->
  key = (hash) -> "upload-cache-#{hash}"

  localCache =
    check: (hash) -> W localStorage[key(hash)]
    put: (hash) -> W localStorage[key(hash)] = true

  search: (path) ->
    operation = new smCore.SearchDownload(path, SettingsController.get('languages'), localCache)
    operation.run()

  scanPath: (path) -> smCore.VideoScan([path])

  checkForUpdates: ->
    nodefn.call(request, pkg.latestJson).then ([res, body]) ->
      json = JSON.parse(body)

      semver.gt(json.version, pkg.version)
