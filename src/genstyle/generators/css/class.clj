(ns genstyle.generators.css.class
  (:require [clojure.spec.alpha :as s]
            [provisdom.spectomic.core :as sp]))

(s/def ::name string?)
(s/def ::genotypes (s/coll-of map?))

(s/def ::entity
  (s/keys :req [::name]
          :opt [::genotypes]))

(def schema
  (sp/datomic-schema
   [::name
    [::genotypes {:db/isComponent true}]]))
