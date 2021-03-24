(ns genstyle.eval
  (:require [sci.core :as sci]))

(def sci-ctx
  (let [bindings (assoc {}
                   'println println)]
    (sci/init
      {:bindings   bindings
       :namespaces {'u (ns-publics 'genstyle.util)}})))

(defn eval-form [form]
  (sci/eval-form sci-ctx form))