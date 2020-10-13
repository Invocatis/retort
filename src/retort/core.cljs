(ns retort.core
  (:require
    [reagent.core :as reagent]
    [retort.hiccup :as hiccup]
    [retort.selector :as selector :refer [selects?]])
  (:refer-clojure :exclude [resolve]))

(defn eldest
  ([context]
   (when context
     (:parent context context)))
  ([context key]
   (when context
     (or (eldest (:parent context) key)
         (get context key)))))

(defn youngest
  [context key]
  (when context
    (if (contains? context key)
      (get context key)
      (recur (:parent context) key))))

(defn ancestory
  [context key]
  (when context
    (lazy-seq
     (cons (get context key) (ancestory (:parent context) key)))))

(defn focus
  [context index]
  (let [children (hiccup/children (:value context))]
    ; (println children (:value context))
    {:parent context
     :siblings children
     :value (nth children index)
     :position index}))

; (defn focus*
;   [context hiccup]
;   (map (partial focus context hiccup) (range (count (hiccup/children hiccup)))))

(defn design-merge
  [& _])

(defn context-defaults
  [hiccup {:keys [position siblings] :or {position 0} :as context}]
  (assoc context :position position :siblings (or siblings (hiccup/children hiccup))))

(defn trim-design
  [design hiccup context]
  (into {} (filter (fn [[k v]] (selects? k hiccup context)) design)))

(defn -select
  [design context hiccup]
  ; (println context)
  (->> design
       (filter (fn [[k _]] (selects? k hiccup context)))
       (into {})))

(def select (memoize -select))

(def cache (atom {}))

(defn derefable?
  [x]
  (satisfies? IDeref x))

(defn wrap-for-deref
  [f selections]
  (if (keyword? f)
    f
    (fn [props & children]
      (apply
        f
        (reduce
         (fn [props k]
           (update props k #(if (derefable? %) (deref %) %)))
         props
         (mapcat keys (vals selections)))
        children))))


(defn fufill
  [hiccup selections state]
  (if (or (empty? selections) (= (get @cache hiccup) selections))
    hiccup
    (let [x
          (into
           [(wrap-for-deref (hiccup/tag hiccup) selections)
            (reduce
             (fn [acc selection]
               (->> selection
                    (map (fn [[k v]]
                           [k (cond
                                (vector? v)
                                (if (empty? v)
                                  state
                                  (if (derefable? state)
                                    (reagent/cursor state v)
                                    (get-in state v)))

                                (fn? v) (apply v hiccup)

                                :else
                                v)]))
                    (into acc)))
             (hiccup/props hiccup)
             (vals selections))]
           (hiccup/children hiccup))]
      (swap! cache assoc hiccup x)
      x)))

(defn resolve
  [[tag & args :as hiccup]]
  (if (fn? tag)
    (apply tag args)
    hiccup))

(defn brew
  [design state hiccup]
  (cond
    (vector? hiccup)
    (cond
      (fn? (first hiccup))
      (let [selections (select design nil hiccup)]
        (if (empty? selections)
          hiccup
          (fufill hiccup selections state)))

      (keyword? (first hiccup))
      (let [selections (select design nil hiccup)]
        (hiccup/update-children
         (if (empty? selections)
           hiccup
           (fufill hiccup selections state))
         (partial map (partial brew design state)))))


    :else
    hiccup))

(declare precompile)

(defn brew-with-precompile
  [design state context hiccup]
  (cond
    (vector? hiccup)
    (cond
      (fn? (first hiccup))
      (let [selections (select design context hiccup)]
        (hiccup/update-tag
         (if (empty? selections)
           hiccup
           (fufill hiccup selections state))
         (partial precompile design state context)))

      (keyword? (first hiccup))
      (let [selections (select design context hiccup)]
        (hiccup/update-children
         (if (empty? selections)
           hiccup
           (fufill hiccup selections state))
         (partial map (fn [i form] (brew-with-precompile design state (focus context i) form)) (range)))))


    :else
    hiccup))

(def precompile-cache
  (atom {}))

(def brew-cache
  (atom {}))

(defn cache!
  [cache key val]
  (swap! cache assoc key val)
  val)

(defn precompile
  [design state context comp]
  (or (get @precompile-cache comp)
      (cache! precompile-cache comp
       (fn [& args]
         (let [hiccup (into [comp] args)
               brewed (-> (brew design state hiccup)
                          resolve
                          (update 0 (fn [tag] (if (fn? tag) (precompile design state context tag) tag))))
               context (assoc context
                              :value brewed
                              :siblings (hiccup/children brewed))]
           (brew-with-precompile design state context brewed))))))
