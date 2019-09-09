(ns retort.wire)

(defn- wire-event
  [attributes [event f]]
  (if (contains? attributes event)
    (update attributes event #(juxt % (partial f attributes)))
    (assoc attributes event (partial f attributes))))

(defn wire
  [plan [tag attr0 & args0 :as hiccup]]
  (reduce
    (fn [hiccup piece]
      (update hiccup 1 wire-event piece))
    hiccup
    plan))
