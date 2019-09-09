(ns retort.selector.compose
  (:require
    [retort.selector.grammar :as grammar]
    [retort.util :refer [html-selector?]]))

(defn compose|map
  [m]
  [:attributes [m]])

(defn compose|vector
  [v]
  [(first v) (subvec v 1)])

(defn compose|keyword
  [element]
  (if-not (html-selector? element)
    [element []]
    (grammar/interpret element)))

(defn compose|element
  [element]
  (cond
    (map? element) (compose|map element)
    (vector? element) (compose|vector element)
    (keyword? element) (compose|keyword element)))

(defn compose
  [selector]
  (loop [selector (if (vector? selector) selector [selector])
         comp {}]
    (if (empty? selector)
      comp
      (recur
        (rest selector)
        (conj comp (compose|element (first selector)))))))
