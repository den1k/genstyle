(ns genstyle.generators.css.site
  (:require [provisdom.spectomic.core :as sp]
            [clojure.spec.alpha :as s]
            [genstyle.generators.css.class :as css-class]
            [genstyle.generators.css.generation :as gen]))

(s/def ::name string?)
(s/def ::generation map?)
(s/def ::css-classes (s/coll-of ::css-class/entity))

(s/def ::entity (s/keys :req [::name] :opt [::generation]))

(def schema
 (sp/datomic-schema [[::name {:db/unique :db.unique/identity}]
                     ::generation
                     [::css-classes {:db/isComponent true}]]))


