(ns genstyle.style
  "A style is like an organism"
  (:require [genstyle.generation :as gen]
            [genstyle.css.ruleset :as ruleset]
            [genstyle.util :as u]
            [genstyle.db.util :as dbu]))

(defn make
  [{:keys [generation genes]}]
  (dbu/with-timestamps
   {::gen/_styles generation
    ::fitness     0
    ::genes       genes}))

(defn make-css
  [{:as opts :keys [selector->props]}]
  (make (assoc opts :genes (mapv ruleset/make selector->props))))

(defn make-css-generation
  [{:as   opts
    :keys [generation-size]
    :or   {generation-size 20}}]
  (let [gen-ent       (gen/make opts)
        opts-with-gen (assoc opts :generation gen-ent)]
    (repeatedly generation-size #(make-css opts-with-gen)))
  )

(defn ent->gen-name [style-ent]
  (-> style-ent ::gen/_styles first ::gen/name))

(comment
  (keys (first (make-css-generation {:selector->props {"sdsd" ["color"]}}))))

;(gen/make {})