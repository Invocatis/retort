(ns example.core
  (:require
    [retort.core :as retort]
    [reagent.core :as r]
    [example.controller :as controller]
    [example.view :as view :refer [view]]))

(def state
  (r/atom {:todos []
           :counter 0}))

(add-watch state :key (fn [k r o n] (println n)))

(defn base []
  (retort/brew
    {:data (controller/data @state)
     :transition (controller/wiring state)}
    [view]))

(defn render []
  (r/render [base]
    (.. js/document (getElementById "app"))))

(render)
