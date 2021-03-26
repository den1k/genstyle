(ns genstyle.entity-utils
  (:require
    [genstyle.project :as project]
    [genstyle.generation :as generation]
    [genstyle.style :as style]
    [genstyle.db :as db]))

(defn gen-ent->project-name [gen-ent]
  (-> gen-ent
      ::generation/styles
      first
      ::project/_population
      first
      ::project/name))

(defn project-generations [proj-name]
  (db/q '[:find [?gen ...]
          :in $ ?proj-name
          :where
          [?p ::project/name ?proj-name]
          [?p ::project/population ?styles]
          [?g ::generation/styles ?styles]
          [(genstyle.db/entity ?g) ?gen]]
        proj-name))

(defn project-generations-desc [proj-name]
  (->>
    (project-generations proj-name)
    (sort-by (comp #(.getTime %) :created-at) >)))

(defn project-latest-generation [proj-name]
  (first (project-generations-desc proj-name)))