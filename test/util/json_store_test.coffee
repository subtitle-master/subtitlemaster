AppInjector.call (JsonStore) ->
  describe "JSON Store", ->
    it "returns null if data is not present", ->
      object = {}
      store = new JsonStore(object)

      expect(store.get('hello')).toBeNull()

    it "reads the incoming encoded data", ->
      data = {a: 'b'}

      object = {data: JSON.stringify(data)}
      store = new JsonStore(object)

      expect(store.get('data')).toEqual(data)

    it "converts data in json to store, parse on restore", ->
      data = {a: 'b'}

      object = {}
      store = new JsonStore(object)

      store.set('data', data)

      expect(object.data).toEqual(JSON.stringify(data))
      expect(store.get('data')).toEqual(data)
