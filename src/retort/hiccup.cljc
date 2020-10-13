(ns retort.hiccup
  (:require
    [clojure.string :as string]
    [clojure.set :as set]
    [retort.selector.grammar :as grammar])
  (:refer-clojure :exclude [destructure]))

(def ^:private html-tag-regex #"^[a-zA-Z0-9\-]+")

(defn- tag->html-tag
  [tag]
  (->> tag name (re-find html-tag-regex) keyword))

(def ^:private class-regex #"(?:[\.])([a-zA-Z0-9\-]+)")

(defn- split-by-character
  [s char]
  (loop [from-index 0
         tokens []]
    (if-let [i (string/index-of s char from-index)]
      (recur (inc i) (conj tokens (subs s from-index i)))
      (conj tokens (subs s from-index)))))

(defn- tag->class-set
  [tag]
  (let [s (name tag)
        class-list (split-by-character s \.)
        last (first (string/split (peek class-list) #"[^a-zA-Z0-9\-]"))]
    (set (rest (conj (pop class-list) last)))))

(def ^:private id-regex #"(?:[\#])([a-zA-Z0-9\-]+)")

(defn- tag->id
  [tag]
  (->> tag name (re-find id-regex) second))

(defn fn-name
  [f]
  (last
    (string/split
      (when f
        #?(:clj (.getName (class f))
           :cljs (.-name f)))
      #"\$")))

(defn tag
  [hiccup]
  (cond
    (keyword? (first hiccup))
    (-> hiccup first tag->html-tag)
    (fn? (first hiccup))
    (first hiccup)
    :else
    nil))

(defn update-tag
  [hiccup f]
  (update hiccup 0 f))

(defn classes
  [hiccup]
  (reduce
    into
    (list
      (if (keyword? (first hiccup))
        (-> hiccup first tag->class-set)
        #{})
      (when (and (map? (second hiccup)) (get (second hiccup) :class))
        (-> hiccup second :class (string/split #"\s+")))
      (-> hiccup first meta :class))))

(defn id
  [hiccup]
  (or
    (when (map? (second hiccup)) (:id (second hiccup)))
    (when (keyword? (first hiccup)) (-> hiccup first tag->id))
    (-> hiccup first meta :id)))

(defn attributes
  [hiccup]
  (when (map? (second hiccup))
      (-> (second hiccup))))

(defn props
  [hiccup]
  (if (map? (second hiccup))
    (second hiccup)
    {}))

(defn prop
  [hiccup prop]
  (-> hiccup props (get prop)))

(defn children
  [hiccup]
  (if (map? (second hiccup))
    (when (> (count hiccup) 2)
      (subvec hiccup 2))
    (when (> (count hiccup) 1)
      (subvec hiccup 1))))

(defn update-children
  [hiccup f]
  (into [(tag hiccup)
         (props hiccup)]
        (f (children hiccup))))

(defn clean
  [hiccup]
  (if (map? (second hiccup))
    hiccup
    (into [(first hiccup) {}] (rest hiccup))))

(defn strip-tag
  [[_ attrs & children :as hiccup]]
  (into
   [(tag hiccup)
    (merge
     attrs
     {:id (id hiccup)
      :class (apply str (classes hiccup))})]
   children))

(defn decompose
  [hiccup]
  {:tag (tag hiccup)
   :props (merge (props hiccup)
                 (when-let [id (id hiccup)]
                   {:id id})
                 (when-let [classes (classes hiccup)]
                   {:class (apply str classes)}))
   :children (children hiccup)})
