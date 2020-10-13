(ns retort.compiler.react
  (:require
   [clojure.string :as string]
   [goog.object :as gobj]
   [retort.core :as retort]
   [retort.compiler :refer [ICompiler -element -component]]
   [retort.hiccup :as hiccup]
   ["react" :as react]
   ["create-react-class" :refer [createReactClass]]))

; (defn reag-element [tag v compiler]
;   (let [c (comp/as-class tag compiler)
;         jsprops #js {}]
;     (set! (.-argv jsprops) v)
;     (when-some [key (util/react-key-from-vec v)]
;       (set! (.-key jsprops) key))
;     (react/createElement c jsprops)))

(defn js-map
  "Makes a javascript map from a clojure one"
  [cljmap]
  (let [out (js-obj)]
    (doall (map #(aset out (name (first %)) (second %)) cljmap))
    out))

(defn jsx->clj
  [x]
  (into {} (for [k (.keys js/Object x)] [(keyword k) (aget x k)])))

(defn make-component
  ([display-name m] (make-component display-name nil m))
  ([display-name construct m]
   (let [cmp (fn [props context updater]
               (cljs.core/this-as this
                 ((.-call react/Component) this props context updater)
                 (when construct
                   (construct this))
                 this))]
     (gobj/extend (.-prototype cmp) (.-prototype react/Component) m)

     (when display-name
       (set! (.-displayName cmp) display-name)
       (set! (.-cljs$lang$ctorStr cmp) display-name)
       (set! (.-cljs$lang$ctorPrWriter cmp)
             (fn [this writer opt]
               (cljs.core/-write writer display-name))))
     (set! (.-cljs$lang$type cmp) true)
     (set! (.. cmp -prototype -constructor) cmp))))

(def create-element react/createElement)

(def my-component
  (make-component "MyComponent"
                  (fn [this] (set! (.-state this) #js{:counter 0}))
                  #js{:render
                      (fn []
                        (this-as this
                          (create-element "div"
                                          nil
                                          #js[(create-element "div"
                                                              #js{:key 1}
                                                              #js["Counter is " (-> this .-state .-counter)])
                                              (create-element "button"
                                                              #js{:key 2
                                                                  :onClick #(.setState this
                                                                                       (fn [old]
                                                                                         #js{:counter (-> old .-counter inc)}))}
                                                              #js["Click me"])])))}))

(defn named?
  [any]
  (instance? cljs.core.INamed any))

(defn snake->camel
  [any]
  (let [tokens (string/split (name any) "-")]
    (apply str (first tokens) (map string/capitalize (rest tokens)))))

(defn rename-keys
  [m name-map]
  (if (empty? name-map)
    m
    (reduce-kv
     (fn [m k v] (assoc m (get name-map k k) v))
     {}
     m)))

(defn munge-props
  [props]
  (reduce-kv
   (fn [m k v]
     (assoc m (snake->camel k) v))
   {}
   (rename-keys props {:class :className})))



(deftype ReactCompiler
  [cache]
  ICompiler
  (-component
    [this design state f]
    (or
     (get cache f)
     (get
      (swap!
       cache
       assoc f
       (make-component
        (if (fn? f) (hiccup/fn-name f) (name f))
        (fn [this] (set! (.-state this) {}))
        #js {:render
             (fn []
               (let [compiler this]
                 (this-as
                  this
                  (let [k (jsx->clj (.. this -props))
                        props (dissoc k :children)
                        children (:children k)]
                    ; (println (hiccup/fn-name f) (.-props this))
                    (if (fn? f)
                      (-element compiler design state (apply f props children))
                      (react/createElement (name f) (clj->js (munge-props props)) (map (partial -element this design state) children)))))))
             :componentDidMount
             (fn []
               ; (println 'mount! (hiccup/fn-name f))
               (this-as
                this
                ; (println '------- (.-key (.-this)))
                (add-watch state this
                  (fn [_ _ _ n]
                    (let [result (retort/fufill [] (retort/select design [f]) n)]
                      (.setState this
                        (fn [old]
                          {:children (:children result)
                           :props (merge (.-props old) (dissoc result :children))})))))))
             :componentWillUnmount
             (fn []
               ; (println 'unmount! (hiccup/fn-name f))
               (this-as this (remove-watch state this)))}))
      f)))

  (-element
    [this design state hiccup]
    (cond
      (vector? hiccup)
      (let [{:keys [tag props children]} (hiccup/decompose hiccup)
            jsprops (js-map (munge-props props))
            children (map (partial -element this design state) children)]
        (set! (.-props jsprops) (munge-props props))

        (when-let [key (:key props (:id props))]
          (set! (.-key jsprops) key))

        (if (fn? tag)
          (react/createElement (-component this design state tag) jsprops children)
          (react/createElement (name tag) jsprops children)))
      (named? hiccup) (name hiccup)
      :else hiccup)))

(defn make-compiler
  []
  (ReactCompiler. (atom {})))
