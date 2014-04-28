module.exports = (nwgui) ->
  {
    openExternalUrl: (url) -> nwgui.Shell.openExternal(url)
  }
