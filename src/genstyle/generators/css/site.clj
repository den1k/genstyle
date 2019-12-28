(ns genstyle.generators.css.site
  (:require [provisdom.spectomic.core :as sp]
            [clojure.spec.alpha :as s]
            [genstyle.generators.css.selector :as css-selector]
            [datahike.api :as d]))

(s/def ::name string?)
(s/def ::css-selectors (s/coll-of ::css-selector/entity))

(s/def ::entity (s/keys :req [::name]))

(def schema
  (sp/datomic-schema [[::name {:db/unique :db.unique/identity}]
                      [::css-selectors {:db/isComponent true}]]))


