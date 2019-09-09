(ns example.view)

(defn todo
  [attributes todo]
  [:div.todo
    [:h2.title (:title todo)]
    [:text.description (:description todo)]
    [:button.complete]])

(def todo-list
  ^{:class "todo-list"}
  (fn [attributes & todos]
    (into
      [:div]
      (map (fn [td] [todo {} td]) todos))))

(defn add-todo
  [attributes]
  [:button.add-todo attributes])

(defn todos
  [attributes]
  [:div.todos {}
    [add-todo {}]
    [todo-list {}]])

(defn counter
  [attributes]
  [:div.counter {}
    [:input]
    [:button.increment "+"]
    [:button.decrement "-"]])


(defn view
  [attributes]
  [:div.view
    [counter {}]])
    ; [todos]])
