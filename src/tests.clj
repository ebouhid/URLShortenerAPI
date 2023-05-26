;; (clojure.test/run-tests 'tests)

(ns tests
    (:require [clojure.test :refer :all]
    [urlshortener]))

(deftest decimal-to-62
    (testing "2009215674938 to base 62 -> zn9edcu"
        (is (= (urlshortener/hashId 2009215674938) "zn9edcu"))))