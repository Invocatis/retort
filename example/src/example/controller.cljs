(ns example.controller)

(defn wiring
  [state]
  {:button.increment {:on-click #(swap! state update :counter inc)}
   :button.decrement {:on-click #(swap! state update :counter dec)}})

(defn data
  [state]
  {:.todo-list {:args (get state :todos)}
   :.counter>input {:value (get state :counter)}})
