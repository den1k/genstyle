(ns genstyle.generators.css.selector
  (:require [clojure.spec.alpha :as s]
            [provisdom.spectomic.core :as sp]))

(s/def ::selector string?)
(s/def ::genotypes (s/coll-of map?))

(s/def ::entity
  (s/keys :req [::selector]
          :opt [::genotypes]))

(def schema
  (sp/datomic-schema
   [::selector
    ::genotypes]))

(defn make [{:keys [selector genotypes]}]
  {::selector  selector
   ::genotypes genotypes})

