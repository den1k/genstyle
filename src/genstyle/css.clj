(ns genstyle.css
  (:require [genstyle.generators.css.genotype :as gt]
            [genstyle.generators.css.phenotype :as pt]
            [genstyle.generators.css.generation :as ggen]
            [genstyle.generators.css.site :as site]
            [genstyle.generators.css.site.instance :as site.instance]
            [genstyle.generators.css.population :as population]
            [genstyle.generators.css.site.style :as site.style]
            [genstyle.generators.css.selector :as css-selector]
            [genstyle.db :as db]
            [garden.core :as garden]
            [datahike.api :as d]
            [genstyle.util :as u]
            [medley.core :as md]
            [clojure.spec.alpha :as s]))

(comment

 (mount.core/start)

 (declare make-site)
 (def app-name "my-app1")
 (def app-ref [::site/name app-name])
 (def site (make-site
            {:name
             app-name
             :instance-n
             10
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
   ::pt/generation 1})

(defn vary-phenotype [gt pt]
  (let [gt (gt/decode-vals (db/entity->map gt))
        pt (pt/decode-vals (db/entity->map pt))]
    (assoc pt ::pt/value (pt/vary-val-multi gt pt))))

(defn format-val [gt pt]
  (let [gt (gt/decode-vals (db/entity->map gt))
        pt (pt/decode-vals (db/entity->map pt))]
    (pt/format-val-multi gt pt)
    ))

(defn format-css
  ([{:as pt ::pt/keys [genotype]}]
   {:pre [genotype]}
   (format-css genotype pt))
  ([{:as gt ::gt/keys [attribute]} pt]
   [(name attribute) (format-val gt pt)]))

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

(defn- css-selector-ents
  "Helper function to create new genotypes or pick existing genotypes for
  provided attribute names at random."
  [css-selector->genotypes-or-attrs]
  (let [attrs->genotypes (attrs->genotypes)]
    (mapv
     (fn [[k v]]
       {:db/id
        (db/tempid)
        ::css-selector/selector
        k
        ::css-selector/genotypes
        (mapv (fn [[type v]]
                (let [gt (case type
                           :css-attr
                           (-> (get attrs->genotypes v)
                               rand-nth
                               (u/ascertain
                                (str "Attr " v " does not exist in DB.
                                              Please add a genotype for it.")))
                           :genotype
                           (-> v
                               (assoc :db/id (db/tempid))
                               (gt/make)))]

                  (assoc gt :db/id (db/tempid))))
              v)})
     css-selector->genotypes-or-attrs)))


(defn make-site
  "Constructs a site entity given a name and a map of css-selectors to genotypes
  or attributes. Will pick a random genotype for an attribute ot throw if one does
  not exist.
  Creates `instance-n` many or 5 instances of styles DNA for the site."
  [{:as opts :keys [instance-n] :or {instance-n 10}}]
  (let [{:keys [name css-selector->genotypes-or-attrs]} (u/conform! ::make-site opts)
        selector-ents (css-selector-ents css-selector->genotypes-or-attrs)
        instances     (mapv
                       (fn [_]
                         (db/with-tempid
                          (site.instance/make
                           {::site.instance/styles
                            (mapv
                             (fn [{:as css-selector
                                   gts ::css-selector/genotypes}]
                               {::site.style/css-selector
                                css-selector
                                ::site.style/phenotypes
                                (mapv (comp pt/encode-vals genotype->phenotype) gts)})
                             selector-ents)})))
                       (range instance-n))]
    [(site/make
      {::site/name          name
       ::site/css-selectors selector-ents
       ::site/population    (population/make
                             {::population/site      [::site/name name]
                              ::population/instances (db/->refs instances)})
       ::site/instances     instances})]))

(comment
 (-> (make-site
      {:name                             "foo4"
       :instance-n                       2
       :css-selector->genotypes-or-attrs {".header" [:border-width]}})
     first
     db/compact))

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
  ([{:keys [db/id] :as site}]
   (-> (db/q '[:find [(pull ?pt [*]) ...]
               :in $ ?site
               :where
               [?site ::site/css-selectors ?cls]
               [?cls ::css-selector/genotypes ?gts]
               [?pt ::pt/genotype ?gts]]
             id)))
  ([{:keys [db/id] :as site} generation]
   (db/q-as-of
    {:find  '[[(pull ?pt [*]) ...]]
     :in    '[?site ?gen]
     :where '[[?site ::site/css-selectors ?cls]
              [?cls ::css-selector/genotypes ?gts]
              [?pt ::pt/genotype ?gts]
              [?pt ::pt/generation ?gen]]}
    id
    generation)))

(defn normalize-fn [nums]
  (let [sum (apply + nums)]
    (assert (pos? sum) "Sum can't be 0.")
    (fn [num]
      (/ num sum))))

(defn wheel-of-fortune
  "Takes a coll of 2-tuples of probability+item.
  Returns a function that upon invocation, simulates a wheel of fortune,
  returning an item based on given probability."
  [probabilities+xs]
  (let [probls  (map first probabilities+xs)
        norm-fn (normalize-fn probls)
        wheel   (->> probabilities+xs
                     (reduce (fn [[slice out] [p x]]
                               (let [sum (+ slice (norm-fn p))]
                                 [sum (conj out [sum x])]))
                             [0 []])
                     second
                     (sort-by first))]
    (fn wheel-fn []
      (let [spin (rand)]
        (some (fn [[slice x]]
                (when (>= slice spin) x))
              wheel)))))

(defn instances-wheel-of-fortune [instances]
  (assert (> (count instances) 2) "Must have at least two instances")
  (wheel-of-fortune (mapv (juxt ::site.instance/rank identity) instances)))

(defn instances->mates-wheel-of-fortune
  "Returns an infinite wheel-of-fortune sequence of site-instance groups for mating.
  Defaults to couples but allows for n-somes through `instance-n.`"
  ([instances]
   (instances->mates-wheel-of-fortune 2 instances))
  ([instance-n instances]
   (let [wheel (instances-wheel-of-fortune instances)]
     (repeatedly
      #(sequence
        (comp (distinct)
              (take instance-n))
        (repeatedly wheel))))))

;; IMPL inspired by
;; https://natureofcode.com/book/chapter-9-the-evolution-of-code/

(defn parents->child-instance [parents]
  (let [selector->selector-ent
        (->> parents
             (mapcat ::site.instance/styles)
             (group-by ::site.style/css-selector))
        styles
        (reduce-kv
         (fn [styles selector-ent style-ents]
           (let [phs (->> style-ents
                          (mapcat ::site.style/phenotypes)
                          ;; crossover
                          (u/rand-nths-by (comp ::gt/attribute ::pt/genotype))
                          db/->refs)]
             (conj styles
                   {::site.style/css-selector (db/->ref selector-ent)
                    ::site.style/phenotypes   phs})))
         []
         selector->selector-ent)]

    (db/with-tempid
     (site.instance/make
      #::site.instance {:styles  styles
                        :parents (db/->refs parents)}
      false))))


(comment
 (declare site-instance->css)


 (->> (ensure-site-ent app-name)
      ::site/instances
      vec
      parents->child-instance
      ;(u/throw-invalid ::site.instance/entity)
      ;vector
      ;(db/with)
      (db/transact->entity db/with)
      ;:db/id
      (def e)
      ;doall
      ;keys
      ;site-instance->css
      ;vector
      ;db/transact
      )

 (site-instance->css (d/pull (d/entity-db e) '[*] (:db/id e)))
 (site-instance->css (d/pull (d/entity-db e2) '[*] (:db/id e)))

 )

(defn next-generation
  [{:keys [site parent-n birth-rate]
    :or   {parent-n 2 birth-rate 1}}]
  "Creates a new generation of children from a site's population.
  Defaults to
    - 2 parents (`parent-n`) per child
    - a `birth-rate` of 1 per instance, resulting in a generation-size equal to
      the current instance count."
  (let [{::site/keys [population]} site
        pop-instances  (::population/instances population)
        pop-count      (count pop-instances)
        next-pop-count (* birth-rate pop-count)
        parent-groups  (take next-pop-count
                             ;; selection
                             (instances->mates-wheel-of-fortune parent-n
                                                                pop-instances))]
    (mapv parents->child-instance parent-groups)))

(defn next-generation!
  [{:as opts :keys [site]}]
  (let [site-id  (:db/id site)
        next-gen (next-generation opts)]
    (db/transact [{:db/id            site-id
                   ::site/population (population/make
                                      {::population/site      site-id
                                       ::population/instances next-gen})
                   ::site/instances  (db/->refs next-gen)}])))

;; mutation
;(defn select)

; *** Site -> CSS

(defn site->garden [{:as site id :db/id}]
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
         vec)))

(defn site-instance->garden [site-instance]
  (letfn [(styles->garden [styles]
            (mapv (fn [{::site.style/keys [css-selector phenotypes]}]
                    (let [sel (::css-selector/selector css-selector)]
                      [sel (u/project format-css phenotypes)]))
                  styles))]
    (->> site-instance ::site.instance/styles styles->garden)))

(defn site-instances->garden [instances]
  (mapv site-instance->garden instances))

(defn site-instance->css [site-instance]
  (-> site-instance site-instance->garden garden/css))

(defn site-instances->css [site-instances]
  (->> site-instances
       (mapv site-instance->css)))

(defn site-population->stylesheets
  [{::site/keys [created-at population] site-id :db/id}]
  (letfn [(instance-css-with-meta-comment
            [{:as          instance
              instance-id  :db/id
              instance-crt ::site.instance/created-at}]
            (str
             "/*\n"
             (with-out-str
               (clojure.pprint/pprint
                {:site-id             site-id
                 :site-created-at     created-at
                 :instance-id         instance-id
                 :instance-created-at instance-crt}))
             "*/\n"
             (site-instance->css instance)))]
    (mapv instance-css-with-meta-comment (::population/instances population))))

(comment
 (next-generation! {:site (db/entity app-ref)})
 (site-population->stylesheets (db/entity app-ref))
 )


(comment

 ;; all populations that ever existed on a site
 (->> (::population/_site (db/entity app-ref))
      (sort-by ::population/created-at))
 )
