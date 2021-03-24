(ns genstyle.generators.css.genotype.util)

(defn random-flip-sign [num]
  ((rand-nth [- +]) num))

(defn bound [num lower-bound upper-bound]
  (cond
    (<= lower-bound num upper-bound) num
    (< num lower-bound) lower-bound
    (> num upper-bound) upper-bound))
