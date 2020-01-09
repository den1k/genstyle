(ns genstyle.db.specs
  (:require [clojure.spec.alpha :as s]))

(s/def ::ref-or-eid-or-ent
  (s/with-gen
   (s/or :eid int?
         :ref (s/tuple keyword? any?)
         :ent map?)
   ;; hacky gen to create ref valueType for datahike
   #(s/gen #{{:db/id "spec-foo"}})))

(s/def ::refs-or-eids-or-ent
  (s/coll-of ::ref-or-eid-or-ent))

