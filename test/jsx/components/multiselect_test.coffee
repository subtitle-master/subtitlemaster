AppInjector.call (MultiSelect, $, _, React) ->
  describe "Multi Select", ->
    afterEach -> React.unmountComponentAtNode(document.body)

    optionToArray = (option) -> [option.innerHTML, option.value]

    readOptionsFrom = (selector) -> _.map($(selector).find('option'), optionToArray)

    readAvailableOptions = -> readOptionsFrom('.multiselect-available')
    readSelectedOptions = -> readOptionsFrom('.multiselect-selected')

    option = (title, value) -> {title, value}

    describe "initialize", ->
      it "have the listed options", ->
        options = [
          option('Option A', 'a')
          option('Option B', 'b')
        ]

        React.renderComponent(MultiSelect({options}), document.body)

        expect(readAvailableOptions()).toEqual [
          ['Option A', 'a']
          ['Option B', 'b']
        ]

      it "shows the selected values on the select options", ->
        options = [
          option('Option A', 'a')
          option('Option B', 'b')
        ]

        selected = ['a', 'c']

        React.renderComponent(MultiSelect({options, selected, hello: 'world'}), document.body)

        expect(readAvailableOptions()).toEqual [
          ['Option B', 'b']
        ]

        expect(readSelectedOptions()).toEqual [
          ['Option A', 'a']
        ]

      it "respects the selected order", ->
        options = [
          option('Option A', 'a')
          option('Option B', 'b')
        ]

        selected = ['b', 'a']

        React.renderComponent(MultiSelect({options, selected, hello: 'world'}), document.body)

        expect(readAvailableOptions()).toEqual []
        expect(readSelectedOptions()).toEqual [
          ['Option B', 'b']
          ['Option A', 'a']
        ]

    describe 'moving options', ->
      render = (selected) ->
        options = [
          option('Option A', 'a')
          option('Option B', 'b')
          option('Option C', 'c')
        ]

        spy = jasmine.createSpy('change spy')

        React.renderComponent(MultiSelect({options, selected: selected, onChange: spy}), document.body)

        spy

      it "does nothing when nothing is selected", ->
        spy = render([])

        $('.multiselect-add').click()

        expect(spy).not.toHaveBeenCalled()

      it "call onChange with new added options", ->
        spy = render(['b'])

        $('.multiselect-available').val(['a'])
        $('.multiselect-add').click()

        expect(spy).toHaveBeenCalledWith(['b', 'a'])

      it "does nothing when nothing is selected to remove", ->
        spy = render([])

        $('.multiselect-remove').click()

        expect(spy).not.toHaveBeenCalled()

      it "call onChange with removed options", ->
        spy = render(['a', 'b'])

        $('.multiselect-selected').val(['b'])
        $('.multiselect-remove').click()

        expect(spy).toHaveBeenCalledWith(['a'])

      describe "sorting", ->
        spy = null

        beforeEach ->
          spy = render(['a', 'b', 'c'])

          $('.multiselect-selected').val(['b'])

        it "moves option up", ->
          $('.multiselect-moveup').click()

          expect(spy).toHaveBeenCalledWith(['b', 'a', 'c'])

        it "moves option down", ->
          $('.multiselect-movedown').click()

          expect(spy).toHaveBeenCalledWith(['a', 'c', 'b'])
