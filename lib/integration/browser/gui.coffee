module.exports = ->
  {
    openExternalUrl: (url) -> window.open(url, '_blank')
    showItemInFolder: (path) -> console.log "Sorry, can't open paths in browser, tried to open:", path
  }
