(defproject retort "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.520"]
                 [motif "1.0.1"]]
  :plugins [[lein-cljsbuild "1.1.4"]]
  :repl-options {:init-ns user}
  :cljsbuild {:dependencies [[reagent "0.8.1"]]
              :builds {:main {:source-paths ["src"]
                              :compiler {:output-to "browser-based/js/main.js"
                                         :optimizations :whitespace
                                         :pretty-print true}}}}
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.3.0-alpha4"]
                                  [proto-repl "0.3.1"]
                                  [criterium "0.4.5"]]}
             :example {:dependencies [[reagent "0.8.1"]]
                       :cljsbuild {:builds
                                    {:main {:source-paths ["example/src"]
                                            :compiler {:output-to "example/resources/public/js/main.js"
                                                       :main example.core
                                                       :optimizations :whitespace
                                                       :pretty-print true}}}}}
             :uberjar {:aot :all}})
