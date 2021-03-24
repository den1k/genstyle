(ns genstyle.css.ruleset
  (:require [genstyle.css.declaration :as decl]))

(defn make
  ([[selector prop-coll]]
   (make selector prop-coll))
  ([selector prop-coll]
   {::selector     selector
    ::declarations (mapv decl/make prop-coll)}))