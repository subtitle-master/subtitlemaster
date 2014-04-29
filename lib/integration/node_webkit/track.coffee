module.exports = (SettingsController, _, request, nodefn) ->
  trackInfo =
    tid: 'UA-3833116-8'
    user: SettingsController.get('uuid')

  appInfo =
    id: 'com.subtitlemaster.nwapp'
    installer: 'com.nwgui.mac'
    name: 'SubtitleMaster'
    version: '0.0.0'

  trackRequest = (type, data) ->
    data = _.defaults data,
      v: 1
      tid: trackInfo.tid
      cid: trackInfo.user
      t: type

    requestOptions =
      url: 'http://www.google-analytics.com/collect'
      method: 'POST'
      form: data

    console.log 'sending track request', requestOptions

    nodefn.call(request, requestOptions)

  {
    screen: (name) ->
      trackRequest('screenview',
        an: appInfo.name
        av: appInfo.version
        aid: appInfo.id
        aiid: appInfo.installer

        cd: name
      )

    search: (search) ->
      console.log 'search event tracking', search
  }
