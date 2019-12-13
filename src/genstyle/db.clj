(ns genstyle.db
  (:require [datahike.api :as d]
            [clojure.java.io :as io]
            [genstyle.db.schema :refer [schema]]
            [genstyle.generators.css.genotype :as gt]
            [mount.core :as mount :refer [defstate]]))

(def db-abs-path (.getAbsolutePath (io/file "resources/db")))
(def uri (str "datahike:file:" db-abs-path))

(defstate conn
 :start (d/connect uri)
 :stop (d/release conn))

(comment
 ;; use
 (mount/start)
 (mount/stop conn)
 (mount/running-states)

 ;; init

 (do
   (d/create-database uri)
   ;(def conn (d/connect uri))
   (d/transact conn schema)
   (d/transact conn gt/initial-genotypes)
   )

 (do
   (d/release conn)
   (d/delete-database uri))

 )
