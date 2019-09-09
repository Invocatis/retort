# retort

retort injects state logic into hiccup data using familiar selector logic.

## Motivation

Much in the way that CSS segregates styling concerns from page structure, retort separates state dependencies and state change functions. Being inspired by CSS, retort uses a similar selector logic to hookup each component with its dependencies.

## Usage

For usage, one should write a plan for data dependencies, and for state changes. These will change as the state itself changes, so wrapping them in a function that takes the application state as a variable is recommended practice.

Let's create a simple increment/decrement program:

```clojure
(require '[retort.core :as retort])

(def state (atom 0))

(def hiccup
  [:div#root
    [:input#value {:type :text}]]
    [:button.increment "+"]
    [:button.decrement "-"])

(defn data
  [state]
  {:input#value {:value state}})

(defn state-changes
  [state]
  {:button.increment {:on-click #(swap! state inc)}
   :button.decrement {:on-click #(swap! state dec)}})

(retort/brew data state-changes hiccup)

;=>   [:div#root
;       [:input#value {:type :number :value 0}]]
;       [:button#increment {:on-click ##theincrementfunction} "+"]
;       [:button#decrement {:on-click ##thedecrementfunction} "-"])

```

Data dependencies can be functions, which will be applied to the parameters of the hiccup component. State change functions will be passed first the parameters of the component, then any event parameters usually passed to the function.

We can refactor some of our code to show this:

```clojure

(def state (atom {:value 0}))

(def hiccup
  [:div#root
    [:input {:path [:value]}]]
    [:button {:apply inc :path [:value]}]
    [:button {:apply dec :path [:value]}])

(defn data
  [state]
  {:input (fn [{:keys [path]}] (get-in state path))
   :button {:args (fn [{:keys [apply]}]
                    (condp = apply
                      inc "+"
                      dec "-"))}})

(defn state-changes
  [state]
  {:button {:on-click (fn [{:keys [apply path]}] (swap! state update-in path apply))
```

## License

Copyright © 2019 FIXME

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
