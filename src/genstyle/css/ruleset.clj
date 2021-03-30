(ns genstyle.css.ruleset
  (:require [genstyle.css.declaration :as decl]
            [genstyle.db :as db]))

(defn make
  ([[selector prop-coll]]
   (make selector prop-coll))
  ([selector prop-coll]
   (db/with-timestamps
    {::selector     (name selector)
     ::declarations (mapv decl/make prop-coll)})))