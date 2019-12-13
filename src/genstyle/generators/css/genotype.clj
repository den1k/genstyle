(ns genstyle.generators.css.genotype
  (:require [genstyle.util :as u]
            [clojure.spec.alpha :as s]
            [provisdom.spectomic.core :as sp]))

(s/def ::attribute keyword?)
(s/def ::value-type keyword?)
(s/def ::value-unit keyword?)
(s/def ::value-variability float?)
(s/def ::possible-values__str string?)
(s/def ::value-range__str string?)
(s/def ::value-ranges__str string?)

(s/def ::entity
  (s/keys :req [::attribute]
          :opt [::value-type
                ::value-unit
                ::value-variability
                ::possible-values__str
                ::value-range__str
                ::value-ranges__str]))

(def schema
  (sp/datomic-schema
   [[::attribute {:db/index true}]
    ::value-type
    ::value-unit
    ::value-variability
    ::possible-values__str
    ::value-range__str
    ::value-ranges__str]))

(defn pick-one [{vals ::possible-values}]
  (rand-nth vals))

(defn pick-closest [{::keys [possible-values]} value]
  (let [vals    (vec (sort possible-values))
        [smaller= bigger] (u/split #(<= % value) vals)
        ; pop included value
        smaller (pop smaller=)]
    (rand-nth [(or (peek smaller) (first vals))
               (or (first bigger) (peek vals))])))

(defn int-from-range
  [{::keys [value-range]}]
  (let [[start end] value-range]
    (+ (rand-int (- end start)) start)))

(defn color [{:as gt ::keys [value-ranges]}]
  (mapv (fn [[from to :as range]]
          (case from
            (:initial :constant) to
            (int-from-range (assoc gt ::value-range range))))
        value-ranges))

(def encode-key-mappings
  [[::possible-values ::possible-values__str]
   [::value-range ::value-range__str]
   [::value-ranges ::value-ranges__str]])

(def decode-key-mappings
  (mapv (comp vec reverse) encode-key-mappings))

(def encode-vals
  (u/rename-update-keys
   {:keys+result-keys encode-key-mappings
    :dissoc-keys?     true
    :f                pr-str}))

(def decode-vals
  (u/rename-update-keys
   {:keys+result-keys decode-key-mappings
    :dissoc-keys?     true
    :f                read-string}))




(def initial-genotypes
  (mapv
   encode-vals
   [{::attribute       :border-style
     ::possible-values ["none" "hidden" "dotted" "dashed" "solid" "double" "groove" "ridge" "inset" "outset"]}
    {::attribute       :font-family
     ::possible-values ["Segoe UI", "Tahoma", "Book Antiqua", "Papyrus", "Helvetica", "Monaco", "Trebuchet MS", "Copperplate", "Calisto MT", "Arial Black", "Didot", "Hoefler Text", "Cambria", "Avant Garde", "Goudy Old Style", "Century Gothic", "Gill Sans", "Arial", "Baskerville", "Geneva", "Lucida Grande", "Verdana", "Brush Script MT", "Lucida Console", "Candara", "Garamond", "Lucida Sans Typewriter", "Franklin Gothic Medium", "Georgia", "Palatino", "Arial Rounded MT Bold", "Lucida Bright", "Futura", "Courier New", "Big Caslon", "Rockwell Extra Bold", "Bodoni MT", "Optima", "Calibri", "Consolas", "Impact", "Arial Narrow", "Andale Mono", "Times New Roman", "Rockwell", "Perpetua"]}
    {::attribute         :font-size
     ::value-type        :int
     ::value-unit        :px
     ::value-variability 0.2
     ::value-range       [10 22]}
    {::attribute         :padding
     ::value-type        :int
     ::value-unit        :px
     ::value-variability 0.2
     ::value-range       [1 20]}
    {::attribute         :margin
     ::value-type        :int
     ::value-unit        :px
     ::value-variability 0.2
     ::value-range       [1 20]}
    {::attribute         :border-width
     ::value-type        :int
     ::value-unit        :px
     ::value-variability 0.2
     ::value-range       [1 10]}
    {::attribute         :border-radius
     ::value-type        :int
     ::value-unit        :px
     ::value-variability 0.2
     ::value-range       [1 10]}
    {::attribute       :font-weight
     ::possible-values [100, 300, 600, 800]}
    {::attribute         :background
     ::value-type        :hsl
     ::value-variability 0.2
     ::value-ranges      [[0 360] [80 100] [:constant 70]]}
    {::attribute         :color
     ::value-type        :hsl
     ::value-variability 0.2
     ::value-ranges      [[0 360] [80 100] [:constant 30]]}]))

(declare ->val-multi)

(defn ->val [gt]
  (-> gt decode-vals ->val-multi))

(defmulti ->val-multi ::attribute)

(defmethod ->val-multi :font-family [gt] (pick-one gt))

(defmethod ->val-multi :font-size [gt] (int-from-range gt))

(defmethod ->val-multi :font-weight [gt] (pick-one gt))
(defmethod ->val-multi :padding [gt] (int-from-range gt))
(defmethod ->val-multi :margin [gt] (int-from-range gt))
(defmethod ->val-multi :border-width [gt] (int-from-range gt))
(defmethod ->val-multi :border-radius [gt] (int-from-range gt))
(defmethod ->val-multi :background [gt] (color gt))
(defmethod ->val-multi :color [gt] (color gt))
(defmethod ->val-multi :border-style [gt] (pick-one gt))

(comment

 (datahike.api/q [:find '[(pull ?e [*])]
                  :where ['?e ::attribute :padding]]
                 (datahike.api/db genstyle.db/conn))

 (datahike.api/q {:find  '[[(pull ?e [*])]]
                  :where ['[?e ::attribute :border-style]]}
                 (datahike.api/db genstyle.db/conn)
                 ))
