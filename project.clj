(defproject lazada "0.1.0-SNAPSHOT"
  :description "Scrapes top 5 products from every category"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]

                 ;; scrape mode libraries
                 [clj-http "0.7.8"]
                 [hickory "0.5.2"]
                 [org.clojure/data.json "0.2.4"]

                 ;; server mode libraries
                 [ring "1.2.0"]
                 [compojure "1.1.5"]]

  :main lazada.core)
