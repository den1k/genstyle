(ns genstyle.db
  (:require [datahike.api :as d]
            [clojure.java.io :as io]
            [genstyle.db.schema :refer [schema]]
            [mount.core :as mount :refer [defstate]]
            [genstyle.util :as u]))

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

(defn entity->map [entity]
  (->> entity doall (into {})))

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

(def entity? datahike.impl.entity/entity?)

(defn compact
  "Helper function for REPL printing that removes namespaces from qualified
  keywords."
  [x]
  (clojure.walk/postwalk
   #(if (keyword? %)
      (keyword (name %))
      %)
   x))


(comment
 ;; use
 (mount/start)
 (mount/stop conn)
 (mount/running-states)

 ;; init

 (d/create-database uri)

 (do
   ;(def conn (d/connect uri))
   (d/transact conn schema)
   (d/transact conn genstyle.generators.css.genotype/initial-genotypes)
   )

 (do
   (d/release conn)
   (d/delete-database uri))

 )
