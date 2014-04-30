Video = require("subtitle-master/lib/video_info.coffee")

module.exports = (grunt) ->
  require("load-grunt-tasks")(grunt)

  grunt.initConfig
    pkg: grunt.file.readJSON("package.json")
    prebuild_path: "prebuild"
    build_path: "build"
    name: "Subtitle Master"

    nodewebkit:
      options:
        version: '0.9.2'
        app_name: '<%= name %>'
        build_dir: './<%= build_path %>'
        mac_icns: './assets/subtitle_master.icns'
        credits: './assets/credits.html'
        mac: true
        win: true
        linux32: true
        linux64: true

        mac_document_types: [
          {
            CFBundleTypeExtensions: Video.extensions
            CFBundleTypeName: "Video"
            CFBundleTypeRole: "Viewer"
          }
          {
            CFBundleTypeName: "Folder"
            CFBundleTypeOSTypes: ["fold"]
            CFBundleTypeRole: "Viewer"
            LSTypeIsPackage: false
          }
        ]

      src: ['<%= prebuild_path %>/**/*']

    json_massager:
      prebuild:
        files:
          "<%= prebuild_path %>/package.json": ["package.json"]
        modifier: (info) ->
          info.window.toolbar = false
          info

    clean:
      prebuild: ["<%= prebuild_path %>"]
      build: ["<%= build_path %>/<%= name %>"]

    copy:
      src:
        expand: true
        cwd: "."
        src: [
          "index.html"
          "stylesheets/app.css"
          "images/**"
          "node_modules/subtitle-master/**"
          "node_modules/request/**"
          "node_modules/node-uuid/**"
          "node_modules/semver/**"
        ]
        dest: "<%= prebuild_path %>"

      main:
        files:
          "<%= prebuild_path %>/boot.js": "./lib/boot_app.js"

    shell:
      options:
        stdout: true

      webpack:
        command: "./node_modules/.bin/webpack lib/main_nw.coffee --output-path <%= prebuild_path %>"

      devbuild:
        command: "./scripts/devbuild"

      devbuildwatch:
        command: "./scripts/devbuildwatch"

    compress:
      mac:
        options:
          archive: "./build/subtitle-master-<%= pkg.version %>-mac.zip"

        files: [
          {cwd: "./build/releases/Subtitle Master/mac", src: "**", expand: true}
        ]

      win:
        options:
          archive: "./build/subtitle-master-<%= pkg.version %>-win.zip"

        files: [
          {cwd: "./build/releases/Subtitle Master/win", src: "**", expand: true}
        ]

      linux32:
        options:
          archive: "./build/subtitle-master-<%= pkg.version %>-linux32.zip"

        files: [
          {cwd: "./build/releases/Subtitle Master/linux32", src: "**", expand: true}
        ]

      linux64:
        options:
          archive: "./build/subtitle-master-<%= pkg.version %>-linux64.zip"

        files: [
          {cwd: "./build/releases/Subtitle Master/linux64", src: "**", expand: true}
        ]

  grunt.registerTask "default", ["nodewebkit", "compress"]
  grunt.registerTask "prebuild", ["clean:prebuild", "shell:webpack", "json_massager:prebuild", "copy:main", "copy:src"]
  grunt.registerTask "build", ["clean:build", "prebuild", "nodewebkit", "compress"]
