(ns genstyle.generators.css.population
  (:require [clojure.spec.alpha :as s]
            [genstyle.util :as u]
            [provisdom.spectomic.core :as sp]
            [genstyle.db.specs :as db.specs]))

(s/def ::created-at int?)
(s/def ::site ::db.specs/ref-or-eid-or-ent)
(s/def ::instances ::db.specs/refs-or-eids-or-ent)

(s/def ::entity (s/keys :req [::created-at ::site ::instances]))

(def schema
  (sp/datomic-schema [::created-at ::site ::instances]))

(defn make [population]
  (u/throw-invalid
   ::entity
   (merge {::created-at (u/inst-ms)}
          population)))

