(ns genstyle.db
  (:require
    [mount.core :as mount :refer [defstate]]
    [datalevin.core :as d]
    [genstyle.util :as u]))

(def schema
  {
   :created-at                              #:db{:valueType :db.type/instant}
   :updated-at                              #:db{:valueType :db.type/instant}

   :parents                                 #:db{:valueType   :db.type/ref
                                                 :cardinality :db.cardinality/many}

   :genstyle.project/name                   #:db{:unique :db.unique/identity}
   :genstyle.project/population             #:db{:valueType   :db.type/ref
                                                 :cardinality :db.cardinality/many}
   :genstyle.style/genes                    #:db{:valueType   :db.type/ref
                                                 :cardinality :db.cardinality/many}
   :genstyle.generation/name                #:db{:unique    :db.unique/identity
                                                 :valueType :db.type/string}
   :genstyle.generation/styles              #:db{:valueType   :db.type/ref
                                                 :cardinality :db.cardinality/many}
   :genstyle.css.ruleset/selector           #:db{:valueType :db.type/string}
   :genstyle.css.ruleset/declarations       #:db{:valueType   :db.type/ref
                                                 :cardinality :db.cardinality/many}
   :genstyle.css.declaration/prop           #:db{:valueType :db.type/string}
   :genstyle.css.declaration/val-instance   #:db{:valueType :db.type/ref}
   :genstyle.css.property.val-instance/kind #:db{:valueType :db.type/keyword}
   })

(defn with-timestamps [{:as m :keys [created-at]}]
  (let [inst (u/date-instant)]
    (cond-> (assoc m :updated-at inst)
      (not created-at) (assoc :created-at inst))))

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