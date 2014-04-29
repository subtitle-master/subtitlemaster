module.exports = ->
  {
    screen: (name) -> console.log 'tracking screen', name
    search: (search) -> console.log 'tracking search', search
    error: (err) -> console.log 'tracking error', err
  }
