(ns genstyle.style
  "A style is like an organism"
  (:require [genstyle.generation :as gen]
            [genstyle.css.ruleset :as ruleset]
            [genstyle.util :as u]
            [genstyle.db :as db]))

(defn make
  [{:keys [generation genes]}]
  (db/with-timestamps
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

(defn ent->gen [style-ent]
  (-> style-ent ::gen/_styles first))

(defn ent->gen-name [style-ent]
  (-> style-ent ent->gen ::gen/name))

(defn ent->project [style-ent]
  (-> style-ent :genstyle.project/_population first))

(defn ent->project-name [style-ent]
  (-> style-ent ent->project :genstyle.project/name))


(comment
  (keys (first (make-css-generation {:selector->props {"sdsd" ["color"]}}))))
