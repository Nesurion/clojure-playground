# clojure-playground

A single page app for recepie management.

## Usage

Start the server with `BASIC_AUTH_USER=some-user BASIC_AUTH_PASSWORD=secret lein run`.
This will take care of the api and persistens on disk.

Then start the figwheel server with `lein figwheel`.
This will monitor the cljc code and regenerate the javascript and hot reload the code.

Then visit `http://localhost:3000`

## License

Copyright © 2017 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
