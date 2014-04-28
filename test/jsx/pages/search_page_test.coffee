AppInjector.call (React, SearchPage, SearchController, $) ->
  describe "Search Page", ->
    beforeEach -> SearchController.clear()
    afterEach -> React.unmountComponentAtNode(document.body)

    describe "rendering", ->
      describe "initial state", ->
        it "shows a message for the user to drop files into the window", ->
          React.renderComponent(SearchPage(), document.body)

          expect($('.center-banner').text()).toEqual('Arraste seus vídeos aqui')

      describe "when there are searches", ->
        it "renders each search", ->
          SearchController.searches = [
            {status: 'init', path: 'a.mkv', key: 1}
            {status: 'init', path: 'b.mkv', key: 2}
          ]

          React.renderComponent(SearchPage(), document.body)

          expect($('body').text()).toMatch('a.mkv')
          expect($('body').text()).toMatch('b.mkv')

    describe "interactions", ->
      describe "removing a search", ->
        it "removes the item", ->
          SearchController.searches = [
            {status: 'init', path: 'a.mkv', key: 1}
          ]

          spyOn(SearchController, 'remove')

          React.renderComponent(SearchPage(), document.body)

          $('.actions .close').click()

          expect(SearchController.remove).toHaveBeenCalledWith(1)
