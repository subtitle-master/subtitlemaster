Subtitle Master
===============

This contains the code for the Subtitle Master application UI.

Downloading
-----------

If you just wanna use the new Subtitle Master go to the [releases](https://github.com/subtitle-master/subtitlemaster/releases) and download a build from there.

Dependencies
------------

These are the dependencies you gonna need to install:

### Java

You need Java 7 or newer installed.

[Click here](https://www.java.com/en/download) to see instructions on how to install Java on your machine.

### Leiningen

You need Leiningen 2 or newer to build the application code.

[Click here](https://github.com/technomancy/leiningen#installation) to see instructions on how to install Leiningen on your machine.
 
### NodeJS

You need NodeJS 0.10 or newer to build the application code.

[Click here](http://nodejs.org) to see instructions on how to install NodeJS on your machine.

### node-webkit

You need node-webkit 0.10 or newer to build the application code.

[Click here](https://github.com/rogerwang/node-webkit) to see instructions on how to install Leiningen on your machine.

Compiling application javascript
-----------------------

Once you have everything installed, first you need to generate the application Javascript:

```
lein cljsbuild once dev
```

That will run and generate the output once, but for development we recommend to use:

```
lein cljsbuild auto dev
```

That way it will auto recompile the output Javascript when any Clojurescript file changes.

Running for development
-----------------------

After you have the Javascript compiled, you just have to run:

```
nw public
```

Dragging videos to dock on development on MacOS
-----------------------------------------------

Because of the way Mac Apps works, in order to be able to accept drop files you have
to have an app with proper Info.plist configuration.

We provide an example file at [resources/Info.plist](https://github.com/subtitle-master/subtitlemaster/tree/master/resources/Info.plist),
you can use this file and replace the `Info.plist` at your `/Applications/node-webkit/Contents/Info.plist`,
note that if you had opened node-webkit during your current session, you may need to restart
the computer in order for the OS to pick up the changes.

Building releases
-----------------

The process of building releases still in development, I'll add more info here once it's on track.

Service Providers
-----------------

Subtitle Master is only possible thanks to some central servers that provides those subtitles, here are the list of currently used providers in Subtitle Master:

- Subdb: http://thesubdb.com/
- Open Subtitles: https://www.opensubtitles.org
