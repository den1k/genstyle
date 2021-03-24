(ns genstyle.db
  (:require [datahike.api :as d]
            [datahike.impl.entity :as de]
            [clojure.java.io :as io]
            [genstyle.db.schema :refer [schema]]
            [mount.core :as mount :refer [defstate]]
            [genstyle.util :as u])
  (:import (datahike.impl.entity Entity)))

(defn- ensure-db-input [inputs]
  (if-let [[i] inputs]
    (if (= i '$)
      inputs
      (into '[$] inputs))
    '[$]))

(def db-abs-path (.getAbsolutePath (io/file "resources/db")))
(def uri (str "datahike:file:" db-abs-path))

(defn ->eid [ent-or-eid]
  (get ent-or-eid :db/id ent-or-eid))

(defstate conn
  :start (d/connect uri)
  :stop (d/release conn))

(defn q [query-exp & inputs]
  (apply d/q query-exp (d/db conn) inputs))

(defn pull
  ([ent-or-eid]
   (pull '[*] ent-or-eid))
  ([selector ent-or-eid]
   (let [eid (->eid ent-or-eid)]
     (d/pull (d/db conn) selector eid))))

(defn q-pull
  "Takes a `where` clause where `?e` is expected to be bound to the entity to
  be pulled."
  ([where-clauses]
   (q-pull where-clauses '[*]))
  ([where-clauses selector]
   (let [q-expr {:find  [(list 'pull '?e selector) '.]
                 :where where-clauses}]
     (q q-expr))))

(defn q-pull-many
  "Takes a `where` clause where `?e` is expected to be bound to the entities to
  be pulled."
  ([where-clauses]
   (q-pull-many where-clauses '[*]))
  ([where-clauses selector]
   (let [q-expr {:find  [[(list 'pull '?e selector) '...]]
                 :where where-clauses}]
     (q q-expr))))

(defn q-as-of
  "Takes a map-based query expression.
  Finds the txInstant of the first entity that matches the bottom clause in
  `where` and performs `q` with a db as-of then."
  [{:as q :keys [find where in]} & args]
  {:pre [find where]}
  (let [in         (ensure-db-input in)
        where-inst (-> where
                       (update (dec (count where)) conj '?tx)
                       (conj '[?tx :db/txInstant ?inst]))
        inst       (u/ascertain
                    (apply d/q
                           {:find  '[?inst .]
                            :in    in
                            :where where-inst}
                           (d/history @conn)
                           args)
                    "No inst found for query")
        tick       (inc (.getTime inst))]
    (apply d/q (assoc q :in in) (d/as-of @conn tick) args)))

(defn entity [ent-or-eid]
  (let [eid (->eid ent-or-eid)]
    (d/entity (d/db conn) eid)))

(declare entity?)

(defn entity->map [ent]
  (cond-> ent
    (entity? ent) (->> doall (into {:db/id (:db/id ent)}))))


(defn entity->ref [{:as ent :keys [id]}]
  (assert id (str "Entity " ent " should have a :db/id."))
  [:db/id id])

(defn- entity-helper-fn [f-or-doall]
  (cond
    (true? f-or-doall) (comp doall entity)
    (false? f-or-doall) entity
    (ifn? f-or-doall) (comp f-or-doall entity)))

(defn q-entity
  "Takes a `where` clause where `?e` is expected to be bound to the entity to be
  returned."
  ([where-clauses]
   (q-entity where-clauses false))
  ([where-clauses f-or-doall]
   (let [q-expr {:find  '[?e .]
                 :where where-clauses}
         f      (entity-helper-fn f-or-doall)]
     (f (q q-expr)))))

(defn q-entities
  "Takes a `where` clause where `?e` is expected to be bound to the entities to be
  returned."
  ([where-clauses]
   (q-entities where-clauses false))
  ([where-clauses f-or-doall]
   (let [q-expr {:find  '[[?e ...]]
                 :where where-clauses}
         f      (entity-helper-fn f-or-doall)]
     (mapv f (q q-expr)))))

(defn transact [tx-data]
  (d/transact conn tx-data))

(defn tempid []
  (d/tempid nil))

(defn with-tempid [ent]
  (cond-> ent (nil? (:db/id ent)) (assoc :db/id (tempid))))

(defn with
  ([tx-data]
   (with @conn tx-data))
  ([db tx-data]
   (d/with db tx-data)))

(defn transact->entity
  "Transacts and returns a single entity as a db entity.
  Two-arity version takes a tx-fn which can be `with` for speculative updates
  on an immutable DB."
  ([ent] (transact->entity transact ent))
  ([tx-fn ent]
   (let [{:as ent id :db/id} (with-tempid ent)
         {:keys [tempids db-after]} (tx-fn [ent])]
     (d/entity db-after (get tempids id)))))

(def entity? de/entity?)

(defn ->ref [ent]
  (u/ascertain (:db/id ent) "Must have :db/id to make ref"))

(defn ->refs [ents]
  (mapv ->ref ents))

(def touch-entity de/touch)

;; *** Helpers

(defn compact
  "Helper function for REPL printing that removes namespaces from qualified
  keywords in collections."
  [x]
  (clojure.walk/postwalk
   (fn [x]
     (cond-> x
       (keyword? x) (-> name keyword)))
   x))

(defn compact-entity
  "Helper function for REPL printing that removes namespaces from qualified
  keywords in collections."
  [x]
  (clojure.walk/prewalk
   (fn [x]
     (cond-> x
       (entity? x) entity->map
       (keyword? x) (-> name keyword)))
   x))

;; REBL
(extend-protocol clojure.core.protocols/Datafiable
  Entity
  (datafy [this]
    (-> this compact-entity)))


(comment
 ;; use
 (mount/start)
 (mount/stop)
 (mount/stop conn)
 (mount/running-states)

 ;; init
 (do
   (mount/stop)
   (d/delete-database uri)
   (d/create-database uri)
   (mount/start)
   (d/transact conn schema)
   (d/transact conn genstyle.generators.css.genotype/initial-genotypes)
   )

 (do
   (d/release conn)
   (d/delete-database uri))

 )
