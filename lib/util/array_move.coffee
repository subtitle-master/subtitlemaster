module.exports = (_) ->
  up: (array, values) ->
    for value in values
      index = array.indexOf(value)
      continue if index == 0 || _.contains(values, array[index - 1])

      [array[index], array[index - 1]] = [array[index - 1], array[index]]

    null

  down: (array, values) ->
    for value in values
      index = array.indexOf(value)
      continue if index == array.length - 1

      [array[index], array[index+1]] = [array[index+1], array[index]]
