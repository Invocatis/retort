(ns retort.compiler)


(defprotocol ICompiler
  (-element [this design state hiccup])
  (-component [this design state f]))
