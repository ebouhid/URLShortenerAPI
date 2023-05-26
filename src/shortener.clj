(ns shortener)

(require '[clojure.set :as set])

(defn digits [n]
  (->> n str (map (comp read-string str))))

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

(defn gen-id []
  (rand-int 350000000))