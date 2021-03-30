(ns genstyle.db
  (:require
    [mount.core :as mount :refer [defstate]]
    [datalevin.core :as d]
    [genstyle.util :as u]
    [datalevin.impl.entity :as de]))

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



(defn make-transact-entity [{:keys [idk->gen]}]
  (let [idks (set (keys idk->gen))]
    (fn transact-entity!
      ([ent-map] (transact-entity! conn [] ent-map))
      ([conn ent-map] (transact-entity! conn [] ent-map))
      ;; could use schema to figure out unique identity keys if there is no db/id
      ;; throw otherwise
      ([conn txs ent-map]
       (let [tempid -1
             [idk id] (some #(find ent-map %) idks)
             inst   (u/date-instant)
             eid    (or (when id [idk id]) (:db/id ent-map))
             {:keys [created-at] db-id :db/id} (some->> eid (d/entity @conn))
             {:keys [db-after tempids]} (->> txs
                                             (into [(cond-> (assoc ent-map
                                                              :db/id (or db-id tempid)
                                                              :created-at (or created-at inst)
                                                              :updated-at inst)
                                                      idk (assoc idk (or id ((get idk->gen idk))))
                                                      )])
                                             (d/transact! conn))
             eid    (or db-id (get tempids tempid))]
         (d/touch (d/entity db-after eid)))))))

(def transact-entity! (make-transact-entity {:idk->gen {}}))

(defn entity->map [ent]
  (when (de/entity? ent)
    (into {:db/id (:db/id ent)} ent)))

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