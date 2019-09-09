(ns retort.wire-test
  (:require [clojure.test :refer :all]
            [retort.wire :refer :all]))

(def test-plan
  {:#id {:asdf :asdf}})

(deftest test1
  (testing "Wiring"))
