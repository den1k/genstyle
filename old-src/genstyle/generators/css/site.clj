(ns genstyle.generators.css.site
  (:require [provisdom.spectomic.core :as sp]
            [clojure.spec.alpha :as s]
            [genstyle.generators.css.selector :as css-selector]
            [genstyle.util :as u]
            [genstyle.db.specs :as db.specs]))

(s/def ::name string?)
(s/def ::css-selectors (s/coll-of ::css-selector/entity))
(s/def ::created-at int?)
(s/def ::instances ::db.specs/refs-or-eids-or-ent)
(s/def ::population ::db.specs/ref-or-eid-or-ent)

(s/def ::entity (s/keys :req [::name ::created-at ::css-selectors ::instances]))

(def schema
  (sp/datomic-schema [[::name {:db/unique :db.unique/identity}]
                      ::created-at
                      ::css-selectors
                      ::instances
                      ::population]))


(defn make [site]
  (u/throw-invalid
   ::entity
   (merge {::created-at (u/inst-ms)}
          site)))
