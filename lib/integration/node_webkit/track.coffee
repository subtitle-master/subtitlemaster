module.exports = (SettingsController, _, request, nodefn, pkg, os) ->
  trackInfo =
    tid: 'UA-3833116-8'
    user: SettingsController.get('uuid')

  appInfo =
    id: 'com.subtitlemaster.nwapp'
    installer: "com.nwgui.#{os.platform()}"
    name: 'SubtitleMaster'
    version: pkg.version

  trackRequest = (type, data) ->
    data = _.defaults data,
      v: 1
      tid: trackInfo.tid
      cid: trackInfo.user
      t: type

      an: appInfo.name
      av: appInfo.version
      aid: appInfo.id
      aiid: appInfo.installer

      cd1: SettingsController.get('languages').join(',')

    requestOptions =
      url: 'http://www.google-analytics.com/collect'
      method: 'POST'
      form: data

    console.log 'sending track request', data

    nodefn.call(request, requestOptions)

  {
    screen: (name) ->
      trackRequest('screenview',
        cd: name
      )

    search: (search) ->
      trackRequest('event',
        ec: 'search',
        ea: search.status
      )

    error: (err) ->
      console.log 'tracking error', err
  }
