config.resolve.alias = {
    "zlib": "browserify-zlib/",
    "stream": "stream-browserify/",
    "https": "https-browserify/",
    "http": "stream-http/",
    "fs": "fs-extra/",
    "constants": "constants-browserify/",
}
webpack = require("webpack")
config.plugins.push(new webpack.ProvidePlugin({
    process: 'process/browser',
    Buffer: ['buffer', 'Buffer'],
}))


