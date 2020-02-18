# webapp-clojure-2020

## Features

### System

- Integrant
- Separate sources for application and development code.

### HTTP Server

- `Immutant` web server with multiple webapps, single port, multiple hostnames.
- Routing: `metosin/reitit`.

### ClojureScript

- Shadow CLJS (lein integration).
- React JS + Rum + Server-side rendering (SSR)

## Installation

1. Leiningen https://leiningen.org/
2. Node.js https://nodejs.org/
3. npm modules: `npm install --no-package-lock`

## Usage

Run for development:

    lein repl

Run to check build with release options:

    lein clean
    lein with-profile test-release run

Build release:

    lein uberjar
    
Run built release:

    java -Dconfig.file=dev-resources/dev/config/default.props -jar ./target/uberjar/website.jar

## Configuration

Custom configuration properties can be placed in optional file (excluded from version control):

    dev-resources/dev/config/user.USERNAME.props