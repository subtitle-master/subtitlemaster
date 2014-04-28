describe "Array Move", ->
  AppInjector.call (arrayMove) ->
    testMoveUp = (move, output) ->
      elements = ["a", "b", "c"]
      arrayMove.up(elements, move)

      expect(elements).toEqual(output)

    it "doesn't do anything if the moving element is on top",            -> testMoveUp(["a"], ["a", "b",  "c"])
    it "doesn't do anything if the moving element is on top - multiple", -> testMoveUp(["a",  "b"], ["a", "b",  "c"])
    it "moves a single element up from the middle",                      -> testMoveUp(["b"], ["b", "a",  "c"])
    it "moves multiple elements from the bottom",                        -> testMoveUp(["b",  "c"], ["b", "c",  "a"])
    it "moves multiple separated elements",                              -> testMoveUp(["a",  "c"], ["a", "c",  "b"])

    testMoveDown = (move, output) ->
      elements = ["a", "b", "c"]
      arrayMove.down(elements, move)

      expect(elements).toEqual(output)

    it "does nothing for an element in the end",            -> testMoveDown(["c"], ["a", "b",  "c"])
    it "does nothing for an element in the end - multiple", -> testMoveDown(["b",  "c"], ["a", "b",  "c"])
    it "moves an element from middle to end",               -> testMoveDown(["b"], ["a", "c",  "b"])
    it "moves an element from top to middle",               -> testMoveDown(["a"], ["b", "a",  "c"])
    it "moves multiple separated elements",                 -> testMoveDown(["a",  "c"], ["b", "a",  "c"])
