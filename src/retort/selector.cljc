(ns retort.selector
  (:require
    [retort.selector.compose :refer [compose]]
    [retort.hiccup :as hiccup]
    [motif.core :refer [matches?]]
    [clojure.string :as string]))

(declare -selects? selects-by-composed?)

(defn- children-selects?
  [hiccup context selector]
  (let [children (hiccup/children hiccup)
        context (assoc context :siblings children)]
    (map-indexed
      (fn [i child]
        (-selects? selector child (assoc context :position i)))
      children)))

(defn fn-name
  [f]
  (last
    (string/split
      (when f
        #?(:clj (.getName (class f))
           :cljs (.-name f)))
      #"\$")))

(defn func
  [[tag] _ func]
  (and (fn? tag) (= (name func) (fn-name tag))))

(defn tag
  [hiccup _ tag]
  (= (hiccup/tag hiccup) tag))

(defn id
  [hiccup _ id]
  (= (hiccup/id hiccup) id))

(defn classes
  [hiccup _ classes]
  (every? (hiccup/classes hiccup) classes))

(defn parent
  [hiccup {:keys [parent] :as context} selector]
  (let [{:keys [position siblings] :as ancestor-context} parent]
    (selects-by-composed? selector (get siblings position) ancestor-context)))

(defn attributes
  [hiccup _ attributes]
  (matches? attributes (hiccup/attributes hiccup)))

(defn first-child
  [hiccup {:keys [position siblings]}]
  (= position 0))

(defn nth-child
  [hiccup {:keys [position]} n]
  (= position n))

(defn nth-last-child
  [hiccup {:keys [siblings position]} n]
  (= position (- (dec (count siblings)) n)))

(defn last-child
  [hiccup {:keys [position siblings]}]
  (= position (dec (count siblings))))

(defn nth-child-of-type
  [hiccup {:keys [siblings position] :as context} n selector]
  (and
    (-selects? selector hiccup context)
    (= n
      (count
       (filter identity
         (map-indexed #(-selects? selector %2 (assoc context :position %1))
           (take position siblings)))))))

(defn nth-last-of-type
  [hiccup {:keys [siblings position] :as context} n selector]
  (and
    (-selects? selector hiccup context)
    (= n
      (count
       (filter identity
         (map-indexed #(-selects? selector %2 (assoc context :position %1))
           (drop (inc position) siblings)))))))

(defn first-child-of-type
  [hiccup {:keys [position siblings] :as context} selector]
  (and
    (-selects? selector hiccup context)
    (every? not
      (map #(-selects? selector % context)
        (take position siblings)))))

(defn last-child-of-type
  [hiccup {:keys [siblings position] :as context} selector]
  (and
    (-selects? selector hiccup context)
    (every? not
      (map #(-selects? selector % context)
        (drop (inc position) siblings)))))

(defn only-of-type
  [hiccup {:keys [siblings position] :as context} selector]
  (and
    (-selects? selector hiccup context)
    (not
      (some identity
        (map-indexed
         #(-selects? selector %2 (assoc context :position %1))
          (concat
            (take position siblings)
            (drop (inc position) siblings)))))))

(defn only-child
  [hiccup {:keys [siblings]}]
  (= (count siblings) 1))

(defn -empty
  [hiccup _]
  (empty? (hiccup/children hiccup)))

(defn n-children
  [hiccup _ n]
  (= (count (hiccup/children hiccup)) n))

(defn every-child
  [hiccup context selector]
  (every? identity (children-selects? hiccup context selector)))

(defn some-child
  [hiccup context selector]
  (some identity (children-selects? hiccup context selector)))

(defn after
  [hiccup {:keys [siblings position] :as context} selector]
  (when-not (zero? position)
    (-selects? selector (nth siblings (dec position))
               (update context :position dec))))

(defn before
  [hiccup {:keys [siblings position] :as context} selector]
  (when-not (= (dec (count siblings)) position)
    (-selects? selector (nth siblings (inc position))
               (update context :position dec))))

(defn preceding
  [hiccup {:keys [siblings position] :as context} selector]
  (some identity
    (map-indexed
      (fn [i sibling]
        (-selects? selector sibling (assoc context :position i)))
      (drop (inc position) siblings))))

(defn following
  [hiccup {:keys [siblings position] :as context} selector]
  (some identity
    (map-indexed
      (fn [i sibling]
        (-selects? selector sibling (assoc context :position i)))
      (take position siblings))))

(defn -not
  [hiccup context selector]
  (not (-selects? selector hiccup context)))

(defn -and
  [hiccup context & selectors]
  (every?
    identity
    (map
      #(-selects? % hiccup context)
      selectors)))

(defn -or
  [hiccup context & selectors]
  (some
    identity
    (map
      #(-selects? % hiccup context)
      selectors)))

(defn -xor
  [hiccup context & selectors]
  (= 1
    (count
      (filter
        identity
        (map
          #(-selects? % hiccup context)
          selectors)))))

(def ^:dynamic selectors
  {:fn func
   :tag tag
   :id id
   :class classes
   :parent parent
   :attributes attributes
   :nth-child nth-child
   :nth-last-child nth-last-child
   :first-child first-child
   :last-child last-child
   :nth-child-of-type nth-child-of-type
   :nth-last-of-type nth-last-of-type
   :first-child-of-type first-child-of-type
   :last-child-of-type last-child-of-type
   :only-of-type only-of-type
   :only-child only-child
   :empty -empty
   :one-child #(n-children %1 %2 1)
   :n-children n-children
   :every-child every-child
   :some-child some-child
   :after after
   :before before
   :preceding preceding
   :following following
   :not -not
   :and -and
   :or -or
   :xor -xor})

(def ^:private mem-compose (memoize compose))

(defn parse-modifier
  [selector]
  (condp = (first (name selector))
    \! [not (keyword (subs (name selector) 1))]
    [identity selector]))

(defn selects-by-composed?
  [composed hiccup context]
  (every?
    (fn [[selector args]]
      (let [[modifier selector] (parse-modifier selector)]
        (modifier (apply (get selectors selector) hiccup context args))))
    composed))

(defn selects?
  [selector hiccup & [context]]
  (let [composed (compose selector)]
    (selects-by-composed? composed hiccup context)))

(def -selects?
  selects?)
