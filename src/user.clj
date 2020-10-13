(ns user
  (:require
    [retort.core :refer [brew]]
    [retort.hiccup :as hiccup]
    [retort.wire :as wire]
    [retort.selector :as s :refer [selects?]]
    [retort.selector.compose :refer [compose]]
    [retort.selector.grammar :refer [interpret]]
    [criterium.core :as crit]))

(defn comp1
  [params & children]
  (into [:div params] children))

(defn comp2
  [params & children]
  [:div params
    [:button "+"]
    [:button "-"]
    (into [:div] children)])

(defn comp3
  [params]
  [:div params
    [:h1.class0 {:attribute 1} "asdf"]])

(defn view
  [params]
  [:div#root.root
    [comp1 {:class "class0"} [:button.b0 "+"] [:div.class1]]
    [comp2 {:class "class1"} [:button.b1 "-"]
      [comp3 {:class "class2"}]]])

(defn data
  [state]
  {:.class0 {:attribute (rand-int 5)}
   :.class1 {:data (rand-int 5)}
   :.class2 {:data (rand-int 5) :attribute (rand-int 5)}
   :div {:something "100"}})
