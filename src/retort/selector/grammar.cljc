(ns retort.selector.grammar
  (:require
    [clojure.string :as string]
    [clojure.set :as set]))

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
        last (first (string/split (peek class-list) #"[^a-zA-Z0-9\-]"))
        classes (set (rest (conj (pop class-list) last)))]
    (when-not (empty? classes)
      classes)))

(def ^:private id-regex #"(?:[\#])([a-zA-Z0-9\-]+)")

(defn- tag->id
  [tag]
  (->> tag name (re-find id-regex) second))

(def ^:private ancesotry-regex #"([^\>])\>|([^\>]+)")

(defn- interpret-single
  [selector]
  (let [tag (tag->html-tag selector)
        id (tag->id selector)
        classes (tag->class-set selector)]
    (conj {}
      (and tag [:tag [tag]])
      (and id [:id [id]])
      (and classes [:class [classes]]))))

(defn- ancestory
  [selector]
  (map
   (fn [[_ x y]] (or x y))
   (re-seq ancesotry-regex (name selector))))

(defn interpret
  [selector]
  (let [ancestors (map interpret-single (ancestory selector))]
    (reduce
     (fn [parent child]
       (assoc child :parent [parent]))
     ancestors)))
