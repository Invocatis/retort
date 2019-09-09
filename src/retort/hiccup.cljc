(ns retort.hiccup
  (:require
    [clojure.string :as string]
    [clojure.set :as set])
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

(defn tag
  [hiccup]
  (when (keyword? (first hiccup))
    (-> hiccup first tag->html-tag)))

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
    (second hiccup)))

(defn children
  [hiccup]
  (if (map? (second hiccup))
    (subvec hiccup 2)
    (subvec hiccup 1)))

(defn clean
  [hiccup]
  (if (map? (second hiccup))
    hiccup
    (into [(first hiccup) {}] (rest hiccup))))
