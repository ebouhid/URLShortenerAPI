(ns urlshortener.core
  (:require [org.httpkit.server :as server]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer :all]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [clojure.set :as set])
  (:gen-class))
;; (load-file "src/shortener.clj")

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
  (defn gen-id [req]
  (rand-int 350000000))

  (defn shorten [req]
    {
      :status 200
      :headers {"Content-Type" "text/html"}
      :body    (str (hash-id (gen-id req)))
    }
  )

  ;; atom manipulation functions
  (defn store-url [id url]
  (swap! url-map assoc id url))

  (defn get-url [id]
  (get @url-map id))

  (defroutes app-routes
  (GET "/" [] simple-body-page)
  (GET "/request" [] request-example)
  (GET "/shorten" [] shorten)
  (GET "/eae" [] hello-name)
  ;; (GET "/redirect" [] )
  (route/not-found "Error, page not found!"))

(defn -main
  "This is our main entry point"
  [& args]
  (def url-map (atom {}))
  (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))]
    ; Run the server with Ring.defaults middleware
    (server/run-server (wrap-defaults #'app-routes site-defaults) {:port port})
    ; Run the server without ring defaults
    ;(server/run-server #'app-routes {:port port})
    (println (str "Running webserver at http:/127.0.0.1:" port "/"))))
