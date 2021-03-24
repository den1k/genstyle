(ns genstyle.generators.css.generation
  (:require [clojure.spec.alpha :as s]
            [provisdom.spectomic.core :as sp]))

(s/def ::count int?)

(s/def ::entity (s/keys :req [::count]))

(def schema
  (sp/datomic-schema [::count]))
