(ns retort.inject)

(defn- invoke-functions
  [data attributes]
  (into {} (map (fn [[k v]] [k (if (fn? v) (v attributes) v)]) data)))

(defn- attributes-of
  [m]
  (if-let [[_ v] (find m :attributes)]
    v
    (when-not (contains? m :args)
      m)))

(defn- inject-attributes
  [data [tag attributes & children :as hiccup]]
  (update hiccup 1 merge (invoke-functions (attributes-of data) attributes)))

(defn- inject-args
  [data [tag attributes & children :as hiccup]]
  (into hiccup (get data :args)))

(defn inject
  [data [tag attr0 & args0 :as hiccup]]
  (->> hiccup (inject-attributes data) (inject-args data)))
