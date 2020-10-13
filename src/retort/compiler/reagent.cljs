(ns retort.compiler.reagent
  (:require
   [clojure.walk :as walk]
   [reagent.impl.protocols :as p]
   [reagent.impl.template :as template]
   [retort.core :as retort]
   [retort.compiler :refer [ICompiler -element -component]]
   [retort.hiccup :as hiccup]
   [retort.util :as util]
   ["react" :as react]))

(def cache (atom {}))
(def cache-element (atom {}))

(def make-cache (atom {}))

(def ^:dynamic context {})

(defn make-element [this argv component jsprops first-child]
  (println component jsprops context)
  (case (- (count argv) first-child)
    ;; Optimize cases of zero or one child
    0 (react/createElement component jsprops)

    1 (react/createElement component jsprops
                           (let [child (nth argv first-child nil)
                                 c context]
                             (binding [context {:parent (-> c
                                                            (assoc :value [(keyword component) (util/jsx->clj jsprops) child])
                                                            (assoc :siblings (if child [child] [])))
                                                :value (nth argv first-child nil)
                                                :position 0
                                                :siblings []}]
                               (p/as-element this (nth argv first-child nil)))))

    (.apply react/createElement nil
            (let [siblings (subvec argv first-child)
                  c context]
              (reduce-kv (fn [a k v]
                           (when (>= k first-child)
                             (binding [context {:parent (-> c
                                                            (assoc :value (into [(keyword component) (util/jsx->clj jsprops)] siblings))
                                                            (assoc :siblings siblings))
                                                :value (nth argv k nil) :position (- k first-child) :siblings []}]
                               (.push a (p/as-element this v))))
                           a)
                         #js [component jsprops] argv)))))

(defn create-compiler [design state opts]
  (let [id (gensym)
        fn-to-element (if (:function-components opts)
                        template/maybe-function-element
                        template/reag-element)]
    (reify p/Compiler
      (get-id [this] id)
      (as-element [this x]
        (if (vector? x)
          (let [selections (retort/select design context x)]
            (if (empty? selections)
              (template/as-element this x fn-to-element)
              (template/as-element this (retort/fufill x selections @state) fn-to-element)))
          (template/as-element this x fn-to-element)))
      (make-element [this argv component jsprops first-child]
        (if-let [cached (get @cache-element [component argv])]
          cached
          (let [element (make-element this argv component jsprops first-child)]
            (swap! cache-element assoc [component argv] element)
            element))))))

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
         (let [hiccup (into [comp] args)]
           ; (println (retort/resolve (retort/brew design state hiccup)))
           ; (println comp
           ; (time
            (or
             ; (when-let [key (hiccup/prop hiccup :key)]
             ;   ; (println key)
             ;   (get brew-cache key))
             (-> (retort/brew design state hiccup)
                 retort/resolve
                 (update 0 (fn [tag] (if (fn? tag) (precompile design state context tag) tag)))
                 (->> (walk/postwalk
                       (fn [x] (if (and (vector? x) (fn? (first x)))
                                 (update x 0 (partial precompile design state))
                                 x)))
                      (mapv (partial retort/brew design state))))))))))
                 ; (->> (cache! brew-cache hiccup)))))))))
