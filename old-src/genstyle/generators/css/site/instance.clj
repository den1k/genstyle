(ns genstyle.generators.css.site.instance
  (:require [provisdom.spectomic.core :as sp]
            [clojure.spec.alpha :as s]
            [genstyle.generators.css.site.style :as style]
            [genstyle.util :as u]))


(s/def ::rank int?)
(s/def ::styles (s/coll-of ::style/entity))
(s/def ::parents (s/coll-of map?))
(s/def ::created-at int?)
(s/def ::entity
  (s/keys :req [::rank ::styles ::created-at]
          :opt [::parents]))

(def schema
  (sp/datomic-schema
   [::rank
    ::parents
    ::created-at
    ::styles]))

(defn make
  ([instance] (make instance true))
  ([instance check?]
   (let [ent (merge
              {::created-at (u/inst-ms)
               ::rank       1}
              instance)]
     (cond->> ent
       check? (u/throw-invalid ::entity)))))
