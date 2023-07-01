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

;; declare db
(def db (d/create-database env/datomic-uri))

;; define connection function
(def conn (d/connect env/datomic-uri))

; define schema
(def url-schema [{:db/ident :id
                  :db/valueType :db.type/string
                  :db/cardinality :db.cardinality/one
                  :db/doc "URL_ID"}
                 {:db/ident :url
                  :db/valueType :db.type/string
                  :db/cardinality :db.cardinality/one
                  :db/doc "URL"}])

    ; transact schema
(d/transact conn url-schema)

;; (def url-map (atom {})) ;; atom declaration
;; ;; atom manipulation functions
;; (defn store-url [id url]
;;   (swap! url-map assoc id url))
;; (defn retrieve-url [hash]
;;   (get @url-map hash))

;; Datomic manimpulation functions
(defn store-url-datomic [id url conn]
  (let [temp-id (d/tempid :db.part/user)
        tx-data [{:id id
                  :url url}]]
    (println "transact out: " (d/transact conn tx-data))
    (println "id: " id)
    (println "url: " url)
    conn))

(defn retrieve-url-datomic [id conn]
  (let [db (d/db conn)
        result (d/q '[:find ?url ?e
                      :in $ ?id
                      :where
                      [?e :id ?id]
                      [?e :url ?url]]
                    db id)]
    (println "queried id: " id)
    (println "result: " result)
    (if (empty? result)
      nil
      (first (first result)))))


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
  (let [id (hash-id (gen-id))
        url (str (:query-string req))]
    (store-url-datomic id url conn)
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body    (str "Shortened URL: " (str "http://127.0.0.1:" 3000 "/redirect?" id))}))

(defn redirect [req]
  (let [hash (str (:query-string req))
        url (retrieve-url-datomic hash conn)]
    (if url
      {:status 302
       :headers {"Location" url}}
      {:status 404
       :headers {"Content-Type" "text/html"}
       :body "URL not found"})))

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
  (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))]

    ; Run the server with Ring.defaults middleware
    (server/run-server (wrap-defaults #'app-routes site-defaults) {:port port})
    (println (str "Running webserver at http:/127.0.0.1:" port "/"))))
