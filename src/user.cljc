(ns user
  (:require
    [retort.core :refer [brew]]
    [retort.hiccup :as hiccup]
    [retort.wire :as wire]
    [retort.selector :as s :refer [selects?]]
    [retort.selector.compose :refer [compose]]
    [retort.selector.grammar :refer [interpret]]))
    ; [criterium.core :as crit]))
