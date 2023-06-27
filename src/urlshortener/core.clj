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
;; (load-file "src/shortener.clj")

(def url-map (atom {}))
  ;; atom manipulation functions
(defn store-url [id url]
  (swap! url-map assoc id url))


; Datomic manimpulation functions


(defn store-url-datomic [id url]
  (let [temp-id (d/tempid :db.part/user)]
    (d/transact (d/connect env/datomic-uri)
                [{:db/id temp-id
                  :url url
                  :id id}])))

(defn retrieve-url-datomic [id]
  (let [result (d/q '[:find ?url
                      :where
                      [?e :id ?id]
                      [?e :url ?url]]
                    (d/db (d/connect env/datomic-uri)))
        id (d/entid (d/db (d/connect env/datomic-uri)) id)]
    (when-let [[url] result]
      url)))
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
    (store-url-datomic id (str (:query-string req)))
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body    id}))

(defn retrieve-url [hash]
  (get @url-map hash))

(defn redirect [req]
  (let [hash (str (:query-string req))]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (retrieve-url-datomic hash)}))

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
        db (d/create-database env/datomic-uri)]
    ; Run the server with Ring.defaults middleware
    (server/run-server (wrap-defaults #'app-routes site-defaults) {:port port})
    ; Run the server without ring defaults
    ;(server/run-server #'app-routes {:port port})
    (println (str "Running webserver at http:/127.0.0.1:" port "/"))))
