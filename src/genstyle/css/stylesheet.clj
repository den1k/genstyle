(ns genstyle.css.stylesheet
  (:require [genstyle.css.ruleset :as ruleset]
            [genstyle.css.declaration :as decl]
            [genstyle.css.property.val-instance :as prop-inst]
            [genstyle.project :as project]
            [genstyle.generation :as gen]
            [garden.core :as garden]
            [genstyle.style :as gstyle]
            [genstyle.db :as db]
            [genstyle.util :as u]))

(defn ruleset->garden
  [{::ruleset/keys [selector declarations]}]
  [selector
   (into {}
         (map (fn [{::decl/keys [prop val-instance]}]
                [prop (::prop-inst/value val-instance)]))
         declarations)])

(defn from-rulesets->garden [rulesets]
  (mapv ruleset->garden rulesets))

(defn from-rulesets [rulesets]
  (garden/css (from-rulesets->garden rulesets)))

(comment
  (println
    (from-rulesets [(ruleset/make ".bridget"
                                  ["color" "padding"])
                    (ruleset/make ".foo"
                                  ["padding"])])))

(defn from-style [{:as style-ent ::gstyle/keys [genes]}]
  (let [meta {:project  (-> style-ent ::project/_population first ::project/name)
              :gen      (gstyle/ent->gen-name style-ent)
              :style-id (:db/id style-ent)}]
    (str
      (u/css-comment (u/pretty-string meta))
      (from-rulesets genes))))

#_(-> (genstyle.db/entity [:genstyle.project/name "foo"])
      :genstyle.project/population first
      ;::gen/_styles first db/touch
      from-style
      println)

(defn sheets-from-project [{::project/keys [population]}]
  (mapv from-style population))

(comment
 (-> (genstyle.db/entity [:genstyle.project/name "foo"])
     sheets-from-project
     println))



