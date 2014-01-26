(ns lazada.core
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.string :as string]
            [hickory.select :as s])
  (:use [hickory.core]
        [lazada.server]))

(def HOMEPAGE_URL "http://www.lazada.com.ph")

;; Some URLs are aliases for other categories and it's easy to enter a loop, so
;; I use an atom to keep record of the URLs I've visited already, and not visit
;; them again

(def visited (atom #{}))

(defn reset-visits! [] (reset! visited #{}))

(defn visit!
  "Add a URL to the set of visited URLs"
  [url]
  (swap! visited conj url))

(defn tree-for
  "Retrieves the content of a given URL and parses them in a hickory-based tree"
  [url]
  (do
    (println (str "GET " url))
    (visit! url)
    (-> (client/get url) :body parse as-hickory)))

(defn full-url
  "Prepends the server homepage URL to a given URI if needed"
  [uri]
  (if (.startsWith uri "http://")
    uri
    (str HOMEPAGE_URL uri)))

(defn subcategories
  "Returns a vector of maps for each sub-category on the current category"
  [tree]
  (let [links (s/select (s/child (s/class "selected") (s/tag :ul) (s/tag :li) (s/tag :a)) tree)]
    (vec
     (map
      #(hash-map :url (-> % :attrs :href full-url)
                 :name (-> % :content first :content first))
      links))))

(defn product-price*
  "Returns the price of a given product in its original format"
  [node]
  (let [deep (-> node :content (nth 3) :content butlast last :content)]
    (-> deep butlast last :content first string/trim)))

(defn product-price
  "Returns a the price of a product as a Number"
  [node]
  (let [formatted-price (product-price* node)
        formatted-amount ((string/split formatted-price #" ") 1)
        decimal-format (java.text.NumberFormat/getInstance java.util.Locale/UK)]
        (.parse decimal-format formatted-amount)))

(defn top5-products
  "Returns a vector of maps describing up to 5 products in a category"
  [cat-tree]
  (let [product-nodes (-> (s/select (s/child (s/class "unit")) cat-tree))]
    (vec
     (map
      #(let [e (-> % :content second :content second)]
         (hash-map
          :image (-> e :content second :attrs :data-image)
          :sku   (-> e :content (nth 3) :content second :content second :attrs :data-sku-simple)
          :url   (-> e :attrs :href full-url)
          :price (product-price e)
          ;; :fmt-price (product-price* e)
          :name  (-> e :attrs :title)))
      (take 5 product-nodes)))))

(defn first-product [cat-tree]
  (first (-> (s/select (s/child (s/class "unit")) cat-tree))))

(defn scrape-categories
  "Given a map with :name and :url keys, finds subcategories and walks them down recursively, selecting the top-5 products in the way"
  [m]
  (let [cat-tree (-> m :url full-url tree-for)
        products (top5-products cat-tree)
        subcats (subcategories cat-tree)
        unvisited-subcats (filter #(-> % :url (@visited) nil?) subcats)
        children (vec (map scrape-categories unvisited-subcats))]
    (if (empty? children)
      {:name (:name m) :url (:url m) :products products}
      {:name (:name m) :url (:url m) :products products :children children})))

(defn save-categories!
  "Serializes a map a JSON string and saves it as the file resources/public/js/categories.json"
  [m]
  (let [output (json/write-str m)]
    (spit "resources/public/js/categories.json" output)))

(def CATEGORY_CLASS "catArrow")

(defn scrape-homepage
  "Scrape the contents of the Lazada homepage and return a nested structure including subcategories and top products in each category"
  []
  (let [homepage-tree (tree-for HOMEPAGE_URL)
        category-nodes (s/select (s/class CATEGORY_CLASS) homepage-tree)
        home-categories (vec
                         (map
                          #(hash-map :url (-> % :attrs :href full-url)
                                     :name (string/trim (-> % :content second :content first)))
                          category-nodes))
        category-trees (vec (map #(scrape-categories %) home-categories))]
    {:name "Homepage" :url HOMEPAGE_URL
     :products (top5-products homepage-tree)
     :children category-trees}))

(defn scrape-mode
  []
  (reset-visits!)
  (println "Starting scrape mode...")
  (println)
  (let [whole-tree (scrape-homepage)]
    (println)
    (println "Scraping complete, saving...")
    (save-categories! whole-tree)))

(defn show-usage
  "Show help about how to use the program"
  []
  (println "Usage: lein run <COMMAND>\n")
  (println "Commands:")
  (println "scrape  Scrapes content of www.lazada.com.ph, generates resources/js/categories.json")
  (println "server  Runs Jetty locally and launches a browser to visualize categories")
  (println))

(defn -main [& args]
  (let [command (first args)]
    (cond
     (= "scrape" command) (scrape-mode)
     (= "server" command) (server-mode)
   :else (show-usage))))

