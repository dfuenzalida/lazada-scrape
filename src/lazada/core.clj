(ns lazada.core
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.string :as string]
            [hickory.select :as s])
  (:use [hickory.core]))

;; REPL:
;; (use 'hickory.core)
;; (require '[hickory.select :as s])
;; (require '[clj-http.client :as client])
;; (require '[clojure.string :as string])

(def HOMEPAGE_URL "http://www.lazada.com.ph")
(def CATEGORY_CLASS "catArrow")
(def SUBCATEGORY_CLASS "fct-category")
(def PRODUCT_CLASS "unit")

(defn tree-for [url]
  (-> (client/get url) :body parse as-hickory))

(def homepage-tree (tree-for HOMEPAGE_URL))

(defn full-url [uri]
  (str HOMEPAGE_URL uri))

(def categories
  (let [tree (tree-for HOMEPAGE_URL)
        category-nodes (s/select (s/class CATEGORY_CLASS) tree)]
    (vec
    (map
     #(hash-map :href (-> % :attrs :href)
                :name (string/trim (-> % :content second :content first)))
     category-nodes))))

(defn subcategories [tree]
  (let [links (s/select (s/child (s/class "selected") (s/tag :ul) (s/tag :li) (s/tag :a)) tree)]
    (vec
     (map
      #(hash-map :url (-> % :attrs :href)
                 :name (-> % :content first :content first))
      links))))

(defn product-price* [node]
  (let [deep (-> node :content (nth 3) :content butlast last :content)]
    (-> deep butlast last :content first string/trim)))

(defn product-price [node]
  (let [formatted-price (product-price* node)
        formatted-amount ((string/split formatted-price #" ") 1)
        decimal-format (java.text.NumberFormat/getInstance java.util.Locale/UK)]
        (.parse decimal-format formatted-amount)))

(defn top5-products [cat-tree]
  (let [product-nodes (-> (s/select (s/child (s/class "unit")) cat-tree))]
    (vec
     (map
      #(let [e (-> % :content second :content second)]
         (hash-map
          :image (-> e :content second :attrs :data-image)
          :sku   (-> e :content (nth 3) :content second :content second :attrs :data-sku-simple)
          :url   (-> e :attrs :href full-url)
          :price (product-price e)
          :fmt-price (product-price* e)
          :name  (-> e :attrs :title)))
      (take 5 product-nodes)))))

(defn first-product [cat-tree]
  (first (-> (s/select (s/child (s/class "unit")) cat-tree))))

;; (def laptop-subtree
;;   (-> (s/select (s/class "fct-category") laptop-tree)
;;       first :content second :content second :content))

;; (def laptop-subcats
;;   (map
;;    #(hash-map :href (-> % :content first :attrs :href)
;;               :text (-> % :content first :content first :content first string/trim))
;;    (drop 1 (butlast laptop-subtree))))

(defn -main [& args]
  (if (empty? args)
    (do
      (println "Usage: java -jar lazada-scaper.jar <COMMAND>\n")
      (println "Commands:")
      (println "scrape - Scrapes content of lazada.com.ph, generates resources/js/categories.json")
      (println "server - Runs jetty locally and launches a browser to visualize categories"))))

;; (defn -main [& args]
;;   (println "loading...")
;;   (println (first category-links)))
