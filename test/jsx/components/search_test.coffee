# states: ['init', 'info', 'upload', 'search', 'download', 'downloaded', 'notfound', 'unchanged', 'uploaded', 'share', 'error']

AppInjector.call (Search, React, $, _) ->
  describe "Search Component", ->
    afterEach -> React.unmountComponentAtNode(document.body)

    describe "rendering states", ->
      renderData = (status, data = {}) ->
        data = _.extend(data, {status, path: '/Some/Path.mkv'})

        React.renderComponent(Search({data}), document.body)

        expect($('.search .info .path').html()).toEqual 'Path.mkv'

      testMessage = (message) ->
        expect($('.search .info .detail').text()).toEqual message

      it "correctly renders init state", ->
        renderData('init')

        testMessage('Iniciando a busca...')

      it "correctly renders info state", ->
        renderData('info')

        testMessage('Carregando informações do vídeo...')

      it "correctly renders upload state", ->
        renderData('upload', {upload: 'video.srt'})

        testMessage('Enviando video.srt ...')

      it "correctly renders the search state", ->
        renderData('search', {search: 'pt, en'})

        testMessage('Buscando legendas nos indiomas: pt, en')

      it "correctly renders the download state", ->
        renderData 'download',
          download:
            source:
              name: -> "Source"

        testMessage('Baixando legenda do servidor: Source...')

      it "correctly renders the downloaded state", ->
        renderData 'downloaded',
          download:
            source:
              name: -> "SubDB"
              website: -> "www.subdb.net"

        testMessage('Baixado do servidor SubDB (www.subdb.net)')

      it "correctly renders the notfound state", ->
        renderData('notfound')

        testMessage('Nenhuma legenda encontrada, tente novamente mais tarde')

      it "correctly renders the unchanged state", ->
        renderData('unchanged')

        testMessage('Você já tem a legenda no seu indioma favorito')

      it "correctly renders the uploaded state", ->
        renderData('uploaded')

        testMessage('Suas legendas locais para esse vídeo foram compartilhadas!')

      it "correctly renders the share state", ->
        renderData('share')

        testMessage('Compartilhando as legendas desse vídeo...')

      it "correctly renders the error state", ->
        renderData('error', error: 'boom!')

        testMessage('Erro: boom!')
