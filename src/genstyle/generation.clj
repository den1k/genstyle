(ns genstyle.generation
  (:require [genstyle.util :as u]
            [genstyle.db :as db]))

(defn make [opts]
  (-> {::name (str "GEN_" (u/name-gen))}
      db/with-timestamps))

(defn entity [name]
  (db/entity [::name name]))

(defn ent->project-name [gen-ent]
  (-> gen-ent
      ::styles
      first
      :genstyle.project/_population
      first
      :genstyle.project/name))