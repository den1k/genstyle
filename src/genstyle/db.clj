(ns genstyle.db
  (:require
    [mount.core :as mount :refer [defstate]]
    [datalevin.core :as d]
    [genstyle.util :as u]
    [genstyle.db.util :as dbu]
    [genstyle.project :as project]
    [genstyle.generation :as gen]
    [genstyle.css.ruleset :as ruleset]
    [genstyle.css.declaration :as decl]
    [genstyle.css.property.val-instance :as prop.inst]
    ))

(def schema
  {:created-at            {:db/valueType :db.type/instant}
   :updated-at            {:db/valueType :db.type/instant}

   ::project/name         {:db/unique :db.unique/identity}
   ::project/population   {:db/valueType   :db.type/ref
                           :db/cardinality :db.cardinality/many}

   ::gen/name             {:db/unique    :db.unique/identity
                           :db/valueType :db.type/string}
   ::gen/styles           {:db/valueType   :db.type/ref
                           :db/cardinality :db.cardinality/many}
   ;; CSS
   ::ruleset/selector     {:db/valueType :db.type/string}
   ::ruleset/declarations {:db/valueType   :db.type/ref
                           :db/cardinality :db.cardinality/many}
   ::decl/prop            {:db/valueType :db.type/string}
   ::decl/val-instance    {:db/valueType :db.type/ref}
   ::prop.inst/kind       {:db/valueType :db.type/keyword}
   ::prop.inst/value      {:db/valueType :db.type/string}})

(def ^:private db-path "data/datalevin/genstyle-db")

(defstate conn
  :start (d/create-conn db-path schema)
  :stop (d/close conn))

(defn transact!
  ([txs]
   (d/transact! conn txs)
   nil)
  ([txs meta]
   (d/transact! conn txs meta)
   nil))

(defn entity [eid]
  (some-> (d/entity @conn eid) d/touch))

(defn q [query & inputs]
  (apply d/q query @conn inputs))

(def touch d/touch)

(defn wipe-db! []
  (mount/stop #'conn)
  (u/delete-directory-recursive db-path)
  (mount/start #'conn)
  :wiped-db!)

(comment
  (wipe-db!)
  )