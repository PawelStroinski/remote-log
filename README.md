# remote-log

A simple logging service with a dynamic front end. Can be used to remotely log from mobile apps.

## Features

* **Secure**: API endpoint intended for adding log entries is MAC-protected. Front end intended for viewing the entries is password-protected with time-limited tokens.
* **Dynamic**: With a help of WebSockets, new entries are automatically revealed on the front end.
* **Roboust**: Entries are stored in the [Cassandra](http://cassandra.apache.org/) database and validated on input with the new [Clojure.spec](http://clojure.org/about/spec).
* **Fun to develop**: Both the back-end and the front-end can be amended with changes visible instantly. This is thanks to, respectively, [Component](https://github.com/stuartsierra/component) ([reloaded workflow](http://thinkrelevance.com/blog/2013/06/04/clojure-workflow-reloaded)) and [figwheel](https://github.com/bhauman/lein-figwheel).

## Roadmap

* Email digest every n days
* Auto-scroll
* More filtering options

## Usage

To run from the REPL, please use `dev/user.clj`. An in-memory database is included.

To get a jar:

    lein uberjar

The `upstart/remote-log.conf` contains all environment variables required to run it.

It has been tested with Cassandra 2.2.6 and 2.1.5.

## License

Copyright © 2016 Paweł Stroiński

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
