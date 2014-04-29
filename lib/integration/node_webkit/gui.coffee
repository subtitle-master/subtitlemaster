module.exports = (nwgui) ->
  {
    openExternalUrl: (url) -> nwgui.Shell.openExternal(url)
    showItemInFolder: (path) -> nwgui.Shell.showItemInFolder(path)
  }
