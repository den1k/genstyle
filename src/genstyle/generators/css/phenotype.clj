(ns genstyle.generators.css.phenotype
  (:require [genstyle.generators.css.genotype :as gt]
            [genstyle.generators.css.genotype.util :as gu]
            [clojure.spec.alpha :as s]
            [provisdom.spectomic.core :as sp]
            [genstyle.util :as u]))

(s/def ::value__str string?)
(s/def ::generation int?)
(s/def ::genotype map?)

(s/def ::entity
  (s/keys :req [::value__str
                ::genotype
                ::generation]))

(def schema
  (sp/datomic-schema
   [::value__str
    ::genotype
    [::generation {:db/valueType :db.type/long}]]))

(def encode-key-mappings
  [[::value ::value__str]])

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


(defn asym
  [{::gt/keys [value-range]} value]
  (let [[minv maxv] value-range]
    (if (zero? value)
      1
      (let [ratio (/ value (if (pos? value) maxv minv))]
        (- 2 ratio)))))

(defn accelerate
  "Scales the value to behave asymptotically towards [min max] in value range."
  [gt value]
  (* value (asym gt value)))

(defn- vary-num [{:as gt ::gt/keys [value-range value-variability]} value]
  (let [[minv maxv] value-range
        rand-variability (* (rand) value-variability)
        value            (accelerate gt value)
        max-delta        (* value rand-variability (asym gt value))
        lower-bound      (max minv (- value max-delta))
        upper-bound      (min maxv (+ value max-delta))
        next-val         (+ value (gu/random-flip-sign max-delta))]
    (gu/bound next-val lower-bound upper-bound)))

(defn vary-int
  [gt value]
  (int (vary-num gt value)))

(defn vary-int-ranges
  [{:as gt ::gt/keys [value-ranges]} {vals ::value}]
  (mapv (fn [[from to :as vrange] value]
          (case from
            :constant to
            (vary-int (assoc gt ::gt/value-range vrange) value)))
        value-ranges
        vals))


(defmulti vary-val-multi (fn [gt _pt] (::gt/attribute gt)))

(defmethod vary-val-multi :font-size
  [gt {val ::value}]
  (vary-int gt val))

(defmethod vary-val-multi :font-family
  [_gt {val ::value}]
  val)

(defmethod vary-val-multi :border-style
  [_gt {val ::value}]
  val)

(defmethod vary-val-multi :padding
  [gt {val ::value}]
  (vary-int gt val))

(defmethod vary-val-multi :margin
  [gt {val ::value}]
  (vary-int gt val))

(defmethod vary-val-multi :border-width
  [gt {val ::value}]
  (vary-int gt val))

(defmethod vary-val-multi :border-radius
  [gt {val ::value}]
  (vary-int gt val))

(defmethod vary-val-multi :font-weight
  [_gt {val ::value}]
  val)

(defmethod vary-val-multi :background
  [gt pt]
  (vary-int-ranges gt pt))

(defmethod vary-val-multi :color
  [gt pt]
  (vary-int-ranges gt pt))

(defn- format-color [{::gt/keys [value-type]} {value ::value}]
  (apply format
         (case value-type
           :rgb "rgb(%d, %d, %d)"
           :hsl "hsl(%d, %d%%, %d%%)")
         value))

(defn css-format [{::gt/keys [value-unit]} val]
  (str val (name value-unit)))

(defmulti format-val-multi ::gt/attribute)

(defmethod format-val-multi :background
  [gt pt]
  (format-color gt pt))

(defmethod format-val-multi :color
  [gt pt]
  (format-color gt pt))

(defmethod format-val-multi :border-style
  [gt {value ::value}]
  value)

(defmethod format-val-multi :font-family
  [gt {value ::value}]
  value)

(defmethod format-val-multi :font-weight
  [gt {value ::value}]
  value)

(defmethod format-val-multi :font-size
  [gt {value ::value}]
  (css-format gt value))

(defmethod format-val-multi :padding
  [gt {value ::value}]
  (css-format gt value))

(defmethod format-val-multi :margin
  [gt {value ::value}]
  (css-format gt value))

(defmethod format-val-multi :border-width
  [gt {value ::value}]
  (css-format gt value))

(defmethod format-val-multi :border-radius
  [gt {value ::value}]
  (css-format gt value))

(defmethod format-val-multi :default
  [cgt pt]
  (println :format-val-default-for cgt pt)
  (::value pt))
