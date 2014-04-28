module.exports = (_, W) ->
  search: (path) ->
    statusData =
      upload: -> "path.mkv"
      search: -> "pt,en"
      download: ->
        source:
          name: -> "SubDB"
          website: -> "www.subdb.net"
      error: -> "boom!"

    flows =
      unchanged: ['init', 'info', 'unchanged']
      upload: ['init', 'info', 'upload', 'share', 'uploaded']
      uploadDownload: ['init', 'info', 'upload', 'search', 'download', 'downloaded']
      download: ['init', 'info', 'search', 'download', 'downloaded']
      missing: ['init', 'info', 'search', 'notfound']
      error: ['init', 'error']

    dataFor = (status) -> statusData[status]?() || null

    W.promise (resolve, reject, notify) ->
      if (flows[path])
        flow = _.clone(flows[path])
      else
        flow = _.clone(_.sample(flows))

      runNext = ->
        status = flow.shift()

        if flow.length == 0
          if status == "error" then reject(status) else resolve(status)
        else
          notify([status, dataFor(status)])

          setTimeout(runNext, _.random(100, 2000))

      runNext()

  scanPath: (path) ->
    W.promise (resolve, reject, notify) ->
      notify({value: path})

      # without the delay the notify is skipped

      setTimeout ->
        resolve(null)
      , 100
