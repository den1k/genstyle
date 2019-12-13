(ns genstyle.db.schema
  (:require [genstyle.generators.css.genotype :as gt]
            [genstyle.generators.css.phenotype :as pt]
            [genstyle.generators.css.generation :as ggen]
            [genstyle.generators.css.site :as site]
            [genstyle.generators.css.class :as css-class]
            [datahike.api :as d]
            [clojure.java.io :as io]))

(def schema
  (into
   []
   cat
   [site/schema
    css-class/schema
    ggen/schema
    gt/schema
    pt/schema]))


(def db-abs-path (.getAbsolutePath (io/file "resources/db")))
(def uri (str "datahike:file:" db-abs-path))

(comment
 (do
   (d/create-database uri)
   (def conn (d/connect uri))
   (d/transact conn schema)
   (d/transact conn gt/initial-genotypes)
   #_(d/transact conn [(u/ffilter (comp #(= % :margin) ::gt/attribute) gt/initial-genotypes)])
   )

 (do
   (d/release conn)
   (d/delete-database uri))

 )



