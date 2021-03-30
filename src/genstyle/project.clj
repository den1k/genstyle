(ns genstyle.project
  (:require
    [genstyle.style :as style]
    [genstyle.css.ruleset :as ruleset]
    [genstyle.css.declaration :as decl]
    [genstyle.db :as db]
    [genstyle.generation :as gen]
    [genstyle.util :as u]
    [net.cgrand.xforms :as x]))

(defn make [name population]
  (db/with-timestamps
    {::name       name
     ::population population}))

(defn make-css-project
  [{:as   opts
    :keys [name]}]
  (make name (style/make-css-generation opts)))

(comment
  (make-css-project
    {:name            "my project"
     :generation-size 20
     :selector->props {".foo" ["color"]}}))

(defn project-generations [proj-name]
  (db/q '[:find [?gen ...]
          :in $ ?proj-name
          :where
          [?p ::name ?proj-name]
          [?p ::population ?styles]
          [?g ::gen/styles ?styles]
          [(genstyle.db/entity ?g) ?gen]]
        proj-name))

(defn project-generations-desc [proj-name]
  (->>
    (project-generations proj-name)
    (sort-by (comp #(.getTime %) :created-at) >)))

(defn project-latest-generation [proj-name]
  (first (project-generations-desc proj-name)))

(defn entity [name]
  (db/entity [::name name]))

(defn alpha-styles [proj-name]
  (let [{::keys [population]} (name->ent proj-name)]
    (sequence
      (comp (remove #(= (::style/fitness %) 0))
            (x/sort-by ::style/fitness >))
      population)))

(defn mate-css-project
  [{:as          opts
    project-name :name
    :keys        [generation-size min-fitness generation-name]
    :or          {generation-size 20 min-fitness 3}}]
  (when-let [gen-eid (:db/id
                       (or (some-> generation-name gen/entity)
                           (project-latest-generation project-name)))]
    (let [res              (db/q
                             '[:find ?style ?sel ?decl
                               :keys style-eid sel decl
                               :in $ ef ?min-fitness ?proj-name ?gen-eid
                               :where
                               [?gen-eid ::gen/styles ?style]
                               [?style ::style/fitness ?fitness]
                               [(>= ?fitness ?min-fitness)]
                               [?style ::style/genes ?rulesets]
                               [?rulesets ::ruleset/selector ?sel]
                               [?rulesets ::ruleset/declarations ?decls]
                               [?decls ::decl/prop ?prop]
                               [(ef ?decls) ?decl]]
                             genstyle.db/entity min-fitness project-name gen-eid)
          gen-ent          (assoc (gen/make opts)
                             :parents [gen-eid])
          grouped-rulesets (u/group-by-map :sel :decl res)
          select-genes     (fn []
                             (map (fn [[sel decls]]
                                    {::ruleset/selector     sel
                                     ::ruleset/declarations (mapv
                                                              (comp
                                                                decl/clone
                                                                rand-nth)
                                                              (vals (group-by ::decl/prop decls)))})
                                  grouped-rulesets))
          parents          (into #{} (map :style-eid) res)]
      {::name       project-name
       ::population (repeatedly
                      generation-size
                      #(assoc
                         (style/make {:generation gen-ent
                                      :genes      (select-genes)})
                         :parents parents))}))
  )

;(mate-css-project {:name "genstyle-landing"})
;(project-latest-generation "genstyle-landing")