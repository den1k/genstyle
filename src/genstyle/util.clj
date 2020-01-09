(ns genstyle.util
  (:require [clojure.spec.alpha :as s]
            [expound.alpha :as exp]
            [net.cgrand.xforms :as x])
  (:refer-clojure :exclude [inst-ms])
  (:import (java.util Date)))

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

(defn project
  ([f coll] (project {} f coll))
  ([to f coll] (into to (map f) coll)))

(defn project-as-keys
  ([key-fn coll]
   (project-as-keys {} key-fn coll))
  ([to key-fn coll]
   (project to (fn [x] [(key-fn x) x]) coll)))

(defn rand-nths-by [f coll]
  (->> coll
       (group-by f)
       (reduce-kv
        (fn [out _ vs]
          (conj out (rand-nth vs)))
        [])))

(defn throw-invalid [spec x]
  (if-not (s/valid? spec x)
    (throw (ex-info "Invalid value"
                    {:value   x
                     :spec    spec
                     :explain (exp/expound spec x)}))

    x))

(defn conform! [spec x]
  (let [conformed (s/conform spec x)]
    (if (= ::s/invalid conformed)
      (throw (ex-info "Invalid value"
                      {:value   x
                       :spec    spec
                       :explain (exp/expound spec x)}))
      conformed)))

(defmacro ascertain
  "Like assert but returns `expr` if it evaluates to logical true"
  ([expr]
   `(ascertain ~expr "Ascertain failed"))
  ([expr msg]
   `(let [ret# ~expr]
      (if-not ret#
        (throw (ex-info ~msg
                        {:expr   '~expr
                         :result ret#}))
        ret#))))

(defn inst []
  (Date.))

(defn inst-ms []
  (clojure.core/inst-ms (inst)))

(defn rand-long [n]
  (long (rand-int n)))
