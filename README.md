# lazada

This project is my implementation of a content scraper for the Lazada website (http://www.lazada.com.ph/) which walks the categories and subcategories available, pulling the top-5 products from each one and assembling a large, nested data structure which is serialized into the file `resources/public/js/categories.json` which is used for visualization.

The code is meant to be run in two phases - the scrape phase, where the website is crawled and the `categories.json` file is generated; and the server phase, in which a simple website based on Jetty serves static files, including a webpage which allows the end user to navigate through the categories tree.

The visualization code is based on "D3.js Drag and Drop, Zoomable, Panning, Collapsible Tree with auto-sizing" by Rob Schmuecker from d3js.org, which I found suitable for this task because it provides collapsable categories, zoom in/out and the input data format was simple to generate from the Clojure code.

## Usage

The code is meant to be run from the command line, and you need to have `lein` installed (leiningen.org).

```
Usage: lein run <COMMAND>

Commands:
scrape  Scrapes content of www.lazada.com.ph, generates resources/js/categories.json
server  Runs Jetty locally and launches a browser to visualize categories
```

## License

Copyright © 2014 Denis Fuenzalida <denis.fuenzalida@gmail.com>

Distributed under the Eclipse Public License, the same as Clojure.
