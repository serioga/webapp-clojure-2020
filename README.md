# webapp-clojure-2020

## Rationale

I needed project setup to support following workflow:

- The application running in development mode behave like application running in production 
  (as much as possible). So all development code is addictive and excluded from production build.
- The application state keep running and reloading automatically on changes.
- Developer can easily extend development environment with any required tools.

So in about 2 years I've gradually built this prototype for my own needs. 

## Features

### Project

- Leiningen based project.
- Code style settings for IntelliJ IDEA with Cursive.

### System

- Integrant for application and development systems.
- Parallel start of integrant components.
- Separate sources for application and development code.
- Hot reloading on source files changes.
- `mount` as integrant component for compiled dependencies in code.
- Configuration in JAVA properties files.
- Daemon interface to be run as service with `jsvc`.  

### HTTP Server

- `Immutant` web server with multiple webapps, single port, multiple hostnames.
- Routing: `metosin/reitit`.
- Page-rendering: `hiccup`.

### Frontend

- ClojureScript with Shadow CLJS (lein integration).
- React JS + Rum + Server-side rendering (SSR) + Passing component data from server
- Tailwind CSS
- Reload pages without Shadow CLJS (adapted `ring-refresh`)

### SQL Database

- `next.jdbc` JDBC wrapper.
- `HugSQL` “query builder”.
- `HikariCP` connection pool.
- Log database queries via `p6spy`.
- Database migrations with `Liquibase`.
- Separate read-write and read-only database connections.

## Q&A

### Why mount _and_ integrant?

During migration of my setup from mount to integrant I found:
- that integrant is good for 
  - managing “big” components like web server, database connection pool and so on, 
    which don't require direct reference in the code,
  - managing multiple systems like application and development once. 
- but passing integrant state around as map to access it from code is
  - annoying,
  - not so performant as mount,
  - harder to trace dependencies in code using IDE's navigation tools.
  
So I decided to take best from both worlds and use mount and integrant simultaneously.   

## Installation

1. OpenJDK 11 https://adoptopenjdk.net/
2. Leiningen https://leiningen.org/
3. Node.js https://nodejs.org/
4. npm modules: `npm install --no-package-lock`

## Usage

Run for development:

    lein run

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
