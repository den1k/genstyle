(ns genstyle.util)

(defn ffilter [pred coll]
  (some #(when (pred %) %) coll))

(defn split [pred coll]
  (loop [yes [] no [] coll coll]
    (if-let [x (first coll)]
      (if (pred (first coll))
        (recur (conj yes x) no (next coll))
        (recur yes (conj no x) (next coll)))
      [yes no])))

(defn rename-update-keys
  ([opts-map]
   (fn [m]
     (rename-update-keys m opts-map)))
  ([m {:keys [keys+result-keys dissoc-keys? f]}]
   (reduce
    (fn [out [k nk]]
      (if-let [v (get out k)]
        (cond-> out
          true (assoc nk (f v))
          dissoc-keys? (dissoc out k))
        out))
    m
    keys+result-keys)))
