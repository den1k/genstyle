(ns genstyle.generators.css.evolve
  (:require [genstyle.generators.css.genotype :as gt]
            [genstyle.generators.css.phenotype :as pt]
            [genstyle.generators.css.generation :as ggen]
            [genstyle.db :as db]
            [datahike.api :as d]))

(defn genotype->phenotype [{:as gt}]
  {::pt/value      (gt/->val gt)
   ::pt/genotype   gt
   ::pt/rank       0
   ::pt/generation {::ggen/count 0}})

(defn vary-phenotype [gt pt]
  (let [gt (gt/decode-vals gt)]
    (assoc pt ::pt/value (pt/vary-val-multi gt pt))))

;; variation √
;; selection √


;; crossover
;; mutation

(for [attr [
            :border-style
            :font-family
            ;:font-size
            :padding
            :margin
            :border-width
            :border-radius
            :font-weight
            :background
            :color
            ]]
  (let [gt (-> (d/q
                {:find  '[[(pull ?e [*])]]
                 :where [['?e ::gt/attribute attr]]}
                (d/db db/conn))
               first
               ;gt/decode-vals
               ;(doto println)
               ;println
               ;genotype->phenotype

               )
        pt (genotype->phenotype gt)
        ]
    (vary-phenotype gt pt)
    ))
