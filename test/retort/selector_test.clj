(ns retort.selector-test
  (:require [clojure.test :refer :all]
            [retort.selector :refer :all]))

(deftest selects?|html-tag
  (testing "selects? html tag"
    (are [selector hiccup] (selects? selector hiccup)
      :div [:div])))

(deftest selects?|wild-card
  (testing "selects? wildcard"
    (are [selector hiccup] (selects? selector hiccup)
      :* [:div]
      :* [:button]
      :*#id.class [:div#id.class]
      :*#id.class [:button#id.class])))

(deftest selects?|class
  (testing "selecsts? class"
    (are [selector hiccup] (selects? selector hiccup)
      :.class1.class2 [:div.class1.class2]
      :.class1.class2 [:div {:class "class1 class2"}]
      :.class1.class2 [:div.class1 {:class "class2"}])))

(deftest selects?|id
  (testing "selecsts? id"
    (are [selector hiccup] (selects? selector hiccup)
      :#id [:div#id]
      :#id [:div {:id "id"}])))

(deftest selects?|child
  (testing "selects? child"
    (are [selector hiccup context] (selects? selector hiccup context)
      [[:nth-child 2]] [:div] {:siblings [[:div] [:div] [:div]] :position 2}
      [[:nth-last-child 1]] [:div] {:siblings [[:div] [:div] [:div]] :position 1}
      :first-child [:div] {:siblings [[:div] [:div]] :position 0}
      :last-child [:div] {:siblings [[:div] [:div]] :position 1}
      :only-child [:div] {:siblings [[:div]]}
      [[:only-of-type :div]] [:div] {:siblings [[:div] [:button] [:p]] :position 0})
    (are [selector hiccup context] (not (selects? selector hiccup context))
      [[:nth-child 0]] [:div] {:siblings [[:div] [:div]] :position 1}
      [[:nth-last-child 1]] [:div] {:siblings [[:div] [:div]] :position 1}
      :first-child [:div] {:siblings [[:div] [:div]] :position 1}
      :last-child [:div] {:siblings [[:div] [:div]] :position 0}
      :only-child [:div] {:siblings [[:div] [:div]]}
      [[:only-of-type :div]] [:div] {:siblings [[:div] [:div]] :position 0})))

(deftest selects?|by-children
  (testing "selects? by children"
    (are [selector hiccup] (selects? selector hiccup nil)
      [:empty] [:div]
      [:empty] [:div {}]
      [:one-child] [:div 1]
      [[:n-children 4]] [:div 1 2 3 4]
      [[:every-child :p]] [:div [:p] [:p] [:p]]
      [[:some-child :p]] [:div [:p] [:button]])
    (are [selector hiccup] (not (selects? selector hiccup nil))
      [:empty] [:div {} 1 2 3]
      [:one-child] [:div 1 2]
      [[:n-children 4]] [:div 1 2 3 4 5]
      [[:every-child :p]] [:div [:p] [:p] [:a]]
      [[:some-child :p]] [:div [:a] [:button]])))

(deftest selects?|child-of-type
  (testing "selects? child of type"
    (are [selector hiccup context] (selects? selector hiccup context)
      [[:nth-child-of-type 1 :div]] [:div] {:siblings [[:div] [:button] [:div]] :position 2}
      [[:nth-last-of-type 0 :div]] [:div] {:siblings [[:div] [:button] [:div]] :position 2}
      [[:first-child-of-type :div]] [:div] {:siblings [[:button] [:div] [:div]] :position 1}
      [[:last-child-of-type :div]] [:div] {:siblings [[:div] [:button] [:div]] :position 2})
    (are [selector hiccup context] (not (selects? selector hiccup context))
      [[:nth-child-of-type 1 :div]] [:div] {:siblings [[:div] [:div] [:div]] :position 2}
      [[:nth-last-of-type 2 :div]] [:div] {:siblings [[:div] [:div] [:div]] :position 2}
      [[:first-child-of-type :div]] [:div] {:siblings [[:button] [:div] [:div]] :position 2}
      [[:last-child-of-type :div]] [:div] {:siblings [[:div] [:button] [:div]] :position 0})))

(deftest selects?|with-parent
  (testing "selects? with parent"
    (are [selector hiccup context] (selects? selector hiccup context)
      :div>button [:button] {:ancestors [{:siblings [[:div]] :position 0}]})
    (are [selector hiccup context] (not (selects? selector hiccup context))
      :div>div [:button] {:ancestors [{:siblings [[:div]] :position 0}]}
      :nav>button [:button] {:ancestors [{:siblings [[:div]] :position 0}]})))

(deftest selects?|siblings
  (testing "selects? siblings"
    (is (selects? [[:after :div]] [:div] {:siblings [[:div] [:div]] :position 1}))
    (is (selects? [[:before :div]] [:div] {:siblings [[:div] [:div]] :position 0}))
    (is (selects? [[:preceding :div]] [:button] {:siblings [[:button] [:a] [:div]] :position 0}))
    (is (selects? [[:following :div]] [:button] {:siblings [[:div] [:a] [:button]] :position 2}))
    (is (not (selects? [[:after :div]] [:div] {:siblings [[:button] [:div]] :position 1})))
    (is (not (selects? [[:before :div]] [:div] {:siblings [[:div] [:button]] :position 1})))
    (is (not (selects? [[:preceding :div]] [:button] {:siblings [[:button] [:a] [:p]] :position 0})))
    (is (not (selects? [[:following :div]] [:button] {:siblings [[:p] [:a] [:button]] :position 2})))))

(deftest selects?|logic
  (testing "selects? logic"
    (are [selector] (selects? selector [:div] {:siblings [[:div]]})
      [[:and :empty :only-child]]
      [[:or :empty :only-child]]
      [[:xor :one-child :only-child]]
      [[:not [[:not :empty]]]])
    (are [selector] (not (selects? selector [:div 1 2 3] {:siblings [[:div] [:div]]}))
      [[:and :empty :only-child]]
      [[:or :empty :only-child]]
      [[:xor :empty :only-child]]
      [[:not [[:not :empty]]]])))
