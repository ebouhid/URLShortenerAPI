(ns urlshortener.core
  (:require [org.httpkit.server :as server]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer :all]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [clojure.set :as set]
            [datomic.api :as d]
            [urlshortener.env :as env])
  (:gen-class))

; define schema
(def url-schema [{:db/ident :url/id
                  :db/valueType :db.type/string
                  :db/cardinality :db.cardinality/one
                  :db/doc "URL_ID"}
                 {:db/ident :url/url
                  :db/valueType :db.type/string
                  :db/cardinality :db.cardinality/one
                  :db/doc "URL"}])
(def url-map (atom {})) ;; atom declaration
;; atom manipulation functions
(defn store-url [id url]
  (swap! url-map assoc id url))
(defn retrieve-url [hash]
  (get @url-map hash))

(def conn (atom nil)) ;; Datomic declaration (will be initialized in -main)
;; Datomic manimpulation functions
(defn store-url-datomic [id url conn]
  (let [temp-id (d/tempid :db.part/user)]
    (d/transact @conn
                [{:url/id (str id)
                  :url/url url}])))

(defn retrieve-url-datomic [id conn]
  (let [db (d/db @conn)
        result (d/q '[:find ?url
                      :where
                      [?e :url/id ?id]
                      [?e :url/url ?url]]
                    db id)]
    (first (first result))))

  ; URL Shortener
; Simple Body Page
(defn simple-body-page [req]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    "Hello World"})

; request-example
(defn request-example [req]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    (->>
             (pp/pprint req)
             (str "Request Object: " req))})

  ; hello-name
(defn hello-name [req] ;(3)
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    (->
             (pp/pprint req)
             (str "Hello " (:name (:params req))))})

  ; hash-id
(defn reverse-str
  [my-str]
  (apply str (reverse my-str)))

(defn hash-id
  [n]
  (let [symbolmap (zipmap (concat
                           (map char (range 48 58))
                           (map char (range 97 123))
                           (map char (range 65 91)))
                          (range 62))]
    (loop [decNumber n
           result []]
      (if (= decNumber 0)
        (reverse-str result)
        (recur (quot decNumber 62)
               (conj result ((set/map-invert symbolmap) (mod decNumber 62))))))))

  ;; gen-id
(defn gen-id []
  (rand-int 350000000))

(defn shorten [req]
  (let [id (hash-id (gen-id))]
    (store-url-datomic id (str (:query-string req)) conn)
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body    id}))

(defn redirect [req]
  (let [hash (str (:query-string req))]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (retrieve-url-datomic hash conn)}))

(defroutes app-routes
  (GET "/" [] simple-body-page)
  (GET "/request" [] request-example)
  (GET "/shorten" [] shorten)
  (GET "/eae" [] hello-name)
  (GET "/redirect" [] redirect)
  (route/not-found "Error, page not found!"))

(defn -main
  "This is our main entry point"
  [& args]
  (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))
        db (d/create-database env/datomic-uri)
        _ (reset! conn (d/connect env/datomic-uri))]

    ; transact schema
    (d/transact @conn url-schema)
    ; Run the server with Ring.defaults middleware
    (server/run-server (wrap-defaults #'app-routes site-defaults) {:port port})
    ; Run the server without ring defaults
    (println (str "Running webserver at http:/127.0.0.1:" port "/"))))
