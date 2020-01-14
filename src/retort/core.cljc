(ns retort.core
  (:require
    [retort.inject :refer [inject]]
    [retort.wire :refer [wire]]
    [retort.hiccup :as hiccup]
    [retort.selector :as selector :refer [selects?]]))

(defn hookup
  [design func hiccup context]
  (loop [design design
         hiccup hiccup
         used []]
    (if-let [[selector values] (first design)]
      (if (selects? selector hiccup context)
        (recur (rest design) (func values hiccup) (conj used selector))
        (recur (rest design) hiccup used))
      [hiccup used])))

(defn brew*
  [design hiccup {:keys [ignore position parent siblings] :as context}]
  (if (vector? hiccup)
    (let [hicc (hiccup/clean hiccup)
          [hicc data-used] (hookup (get design :data)
                             inject hicc context)
          [hicc transition-used] (hookup (get design :transition)
                                   wire hicc context)]
      (cond
        (fn? (first hicc))
        (let [evaluated (apply (first hicc) (rest hicc))]
          (brew* design evaluated
            (-> context
              (assoc-in [:siblings position] evaluated))))
        ((some-fn keyword? symbol? ifn?) (first hicc))
        (let [children (hiccup/children hicc)]
          (into (subvec hicc 0 2)
            (map-indexed
              (fn [i child]
                (brew* design child {:position i
                                     :parent context
                                     :siblings children}))
              children)))
        :else hiccup))
    hiccup))

(def mem-brew (memoize brew*))

(defn brew
  [design hiccup]
  (mem-brew design hiccup {:position 0 :siblings [hiccup]}))
