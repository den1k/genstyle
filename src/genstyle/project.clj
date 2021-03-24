(ns genstyle.project
  (:require
    [genstyle.style :as style]
    [genstyle.db.util :as dbu]))

(defn make [name population]
  (dbu/with-timestamps
   {::name       name
    ::population population}))

(defn make-css-project
  [{:as   opts
    :keys [name]}]
  (make name (style/make-css-generation opts)))


(comment
  (make-css-project
    {:name            "my project"
     :generation-size 20
     :selector->props {:foo ["color"]}}))
