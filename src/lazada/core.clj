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

;; (def LAPTOPS_URL "http://www.lazada.com.ph/shop-computers-laptops/")
;; (def laptop-tree (tree-for LAPTOPS_URL))

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
  (let [subcat-tree (-> (s/select (s/class "fct-category") tree)
                        first :content second :content second :content)]
    (vec
     (map
      #(hash-map :url (-> % :content first :attrs :href)
                 :name (-> % :content first :content first :content first string/trim))
      (drop 1 (butlast subcat-tree))))))

(defn product-name [node]
  (-> node :content second :content second :attrs :title))

(defn product-url [node]
  (-> node :content second :content second :attrs :href full-url))

(defn product-image-url [node]
  (-> node :content second :content second :content second :attrs :data-image))

(defn product-sku [node]
  (-> node :content second :content second :content (nth 3) :content second
      :content second :attrs :data-sku-simple))

(defn product-price* [node]
  (let [deep (-> node :content second :content second :content (nth 3) :content butlast last :content)]
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
      #(hash-map
        :image (product-image-url %)
        :sku   (product-sku %)
        :url   (product-url %)
        :price (product-price %)
        :name  (product-name %))
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
  (println "loading...")
  (println (first category-links)))
