(ns genstyle.generators.css.site.style
  (:require [clojure.spec.alpha :as s]
            [provisdom.spectomic.core :as sp]
            [genstyle.generators.css.selector :as css-selector]
            [genstyle.generators.css.phenotype :as pt]))

(s/def ::css-selector ::css-selector/entity)
(s/def ::phenotypes (s/coll-of ::pt/entity))

(s/def ::entity (s/keys :req [::css-selector ::phenotypes]))

(def schema
  (sp/datomic-schema
   [::css-selector
    ::phenotypes]))

