(ns retort.core-test
  (:require [clojure.test :refer :all]
            [retort.core :refer :all]))

(defn div
  [params & children]
  (into [:div.class params] children))

(deftest brew|data
  (testing "brew test"
    (are [hiccup data result] (= (brew {:data data} hiccup) result)
      [:div] {:div {:a 1}} [:div {:a 1}]
      [:div.class] {:.class {:a 1}} [:div.class {:a 1}]
      [:div [:div]] {:div>div {:a 1}} [:div {} [:div {:a 1}]]
      [:a.c1 [:a.c2 [:a.c3]]] {:.c1>.c2>.c3 {:a 1}} [:a.c1 {} [:a.c2 {} [:a.c3 {:a 1}]]]
      [:div.class [:div]] {:.class>div {:a 1}} [:div.class {} [:div {:a 1}]]
      [div [:input]] {:.class>input {:a 1}} [:div.class {} [:input {:a 1}]]
      [div [div [:input]]] {:.class>input {:a 1}} [:div.class {} [:div.class {} [:input {:a 1}]]]
      [:div] {:div {:args [1 2 3]}} [:div {} 1 2 3])))
