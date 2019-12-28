(ns genstyle.css
  (:require [genstyle.generators.css.genotype :as gt]
            [genstyle.generators.css.phenotype :as pt]
            [genstyle.generators.css.generation :as ggen]
            [genstyle.generators.css.site :as site]
            [genstyle.generators.css.selector :as css-selector]
            [genstyle.db :as db]
            [garden.core :as garden]
            [datahike.api :as d]
            [genstyle.util :as u]
            [medley.core :as md]
            [clojure.spec.alpha :as s]))

(comment

 (def app-name "my-app3")
 (def site (make-site {:name
                       app-name
                       :css-selector->genotypes-or-attrs
                       {".header"    [:font-size
                                      :font-family
                                      :font-weight
                                      :color]
                        ".section-a" [:font-size
                                      :border-width]
                        ".section-b" [:font-size
                                      :border-radius
                                      :border-width]}}))

 (db/compact site)
 (db/transact site)


 (->
  (site-by-name app-name)
  db/compact
  )

 )

; *** Helper fns

(defn genotype->phenotype [{:as gt}]
  {::pt/value      (gt/->val gt)
   ::pt/genotype   gt
   ::pt/rank       0
   ::pt/generation 1})

(defn vary-phenotype [gt pt]
  (let [gt (gt/decode-vals gt)
        pt (pt/decode-vals pt)]
    (assoc pt ::pt/value (pt/vary-val-multi gt pt))))

(defn format-val [gt pt]
  (let [gt (gt/decode-vals gt)
        pt (pt/decode-vals pt)]
    (pt/format-val-multi gt pt)
    ))

(defn format-css [{:as gt ::gt/keys [attribute]} pt]
  [(name attribute) (format-val gt pt)])

; *** Make Site

(s/def ::name string?)
(s/def ::css-selector->genotypes-or-attrs
  (s/map-of
   ::css-selector/selector
   (s/*
    (s/or :genotype map?
          :css-attr ::gt/attribute))))

(s/def ::make-site
  (s/keys :req-un [::name]
          :opt-un [::css-selector->genotypes-or-attrs]))

(defn genotypes []
  (db/q-pull-many '[[?e ::gt/attribute]]))

(defn attrs->genotypes []
  (group-by ::gt/attribute (genotypes)))

(defn- css-selector->genotypes-or-attrs-entities
  "Helper function to create new genotypes or pick existing genotypes for
  provided attribute names at random."
  [css-selector->genotypes-or-attrs]
  (let [attrs->genotypes (attrs->genotypes)]
    (md/map-vals
     #(mapv (fn [[type v]]
              (let [gt (case type
                         :css-attr (-> (get attrs->genotypes v)
                                       rand-nth
                                       (u/ascertain
                                        (str "Attr " v " does not exist in DB.
                                              Please add a genotype for it.")))
                         :genotype (-> v
                                       (assoc :db/id (db/tempid))
                                       (gt/make)))]

                (assoc gt :db/id (db/tempid))))
            %)
     css-selector->genotypes-or-attrs)))

(defn make-site [opts]
  (let [{:keys [name css-selector->genotypes-or-attrs]} (u/conform! ::make-site opts)
        class->genotypes (css-selector->genotypes-or-attrs-entities
                          css-selector->genotypes-or-attrs)
        gts              (into [] cat (vals class->genotypes))
        pts              (mapv #(-> %
                                    genotype->phenotype
                                    (assoc ::pt/generation 1)
                                    pt/encode-vals)
                               gts)]
    (into
     [{::site/name          name
       ::site/css-selectors (reduce-kv
                             (fn [out selector gts]
                               (conj out (css-selector/make {:selector selector :genotypes gts})))
                             []
                             class->genotypes)}]
     pts)))

(defn site-by-name [name]
  (db/pull [::site/name name]))

(defn- ensure-site-ent [name-or-ent]
  (cond
    (db/entity? name-or-ent) name-or-ent
    (string? name-or-ent) (db/entity [::site/name name-or-ent])
    ;; lookup ref or id
    :else (db/entity name-or-ent)))

; *** Selection

(defn phenotypes
  ([site]
   (let [{:keys [db/id]} (ensure-site-ent site)]
     (-> (db/q '[:find [(pull ?pt [*]) ...]
                 :in $ ?site
                 :where
                 [?site ::site/css-selectors ?cls]
                 [?cls ::css-selector/genotypes ?gts]
                 [?pt ::pt/genotype ?gts]]
               id))))
  ([site generation]
   (let [{:keys [db/id]} (ensure-site-ent site)]
     (db/q-as-of
      {:find  '[[(pull ?pt [*]) ...]]
       :in    '[?site ?gen]
       :where '[[?site ::site/css-selectors ?cls]
                [?cls ::css-selector/genotypes ?gts]
                [?pt ::pt/genotype ?gts]
                [?pt ::pt/generation ?gen]]}
      id
      generation))))

(comment

 ;; random phenotype ranking
 (def pts (phenotypes app-name))
 (db/compact pts)

 (def next-pts
   (mapv #(update % ::pt/rank + (- (rand-int 10) 5)) pts))

 (db/compact next-pts)

 (let [spts      (sort-by ::pt/rank > next-pts)
       survivors (take (/ (count spts) 2) spts)]
   )

 )


;; selection
;; crossover: pair with other successful
;; mutation
;(defn select)

; *** Site -> CSS

(defn site->garden [site]
  (when-let [id (:db/id (ensure-site-ent site))]
    (letfn [(selector->styles [selectors+gts+pts]
              (reduce
               (fn [out [selector gt pt]]
                 (update out selector merge (format-css gt pt)))
               {}
               selectors+gts+pts))]
      (->> (db/q '[:find
                   ?cls-name
                   (pull ?gts [*])
                   (pull ?pts [*])
                   :in $ ?site
                   :where
                   [?site ::site/css-selectors ?cls]
                   [?cls ::css-selector/selector ?cls-name]
                   [?cls ::css-selector/genotypes ?gts]
                   [?pts ::pt/genotype ?gts]]
                 id)
           selector->styles
           vec))))


(defn site->css [site]
  (-> (site->garden site)
      garden/css))

(comment
 (site->css [::site/name app-name]))





;; selection








