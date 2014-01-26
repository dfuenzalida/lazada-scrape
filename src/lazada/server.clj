(ns lazada.server
  (:require [ring.adapter.jetty :as jetty]
            [ring.util.response :as response]
            [compojure.core :as compojure]
            [compojure.route :as route])
  (:use [clojure.java.browse]
        [ring.middleware.resource :only [wrap-resource]]))

;; Routes

(compojure/defroutes routes
  (route/resources "/")
  (route/not-found "Page not found"))

;; Middleware

(defn wrap-logging [handler]
  (fn [{:keys [headers remote-addr request-method uri] :as request}]
    (let [remote (or (get headers "x-real-ip") remote-addr)]
      (println remote (clojure.string/upper-case (name request-method)) uri)
      (handler request))))

(defn wrap-root-index [handler]
  (fn [{:keys [uri] :as request}]
    (if (or (= "" uri)
            (= "/" uri)
            (= "/index.html" uri))
      (handler (assoc request :uri "/index.html"))
      (handler request))))

;; Web app config

(def app (-> routes
             wrap-root-index
             wrap-logging))

;; Server

(defn server-mode []
  (println "Starting server mode...")
  (-> (Thread.
       (do
         (Thread/sleep 3000)
         (println "Launching browser...")
         (browse-url "http://0.0.0.0:8000/"))) .start)
  (jetty/run-jetty app {:port 8000}))

