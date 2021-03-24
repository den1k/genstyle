(ns genstyle.generation
  (:require [genstyle.util :as u]
            [genstyle.db.util :as dbu]))

(defn make [opts]
  (-> {::name (str "GEN_" (u/name-gen))}
      dbu/with-timestamps))