module.exports = {
  entry: "./lib/main.coffee",
  output: {
    path: __dirname,
    filename: "sm-client.js"
  },

  module: {
    loaders: [
      {test: /\.coffee$/, loader: 'coffee-loader'},
      {test: /\.jsx$/, loader: 'jsx-loader'}
    ]
  }
};
