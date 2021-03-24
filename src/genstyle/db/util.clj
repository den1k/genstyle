(ns genstyle.db.util
  (:require [genstyle.util :as u]))

(defn with-timestamps [{:as m :keys [created-at]}]
  (let [inst (u/date-instant)]
    (cond-> (assoc m :updated-at inst)
      (not created-at) (assoc :created-at inst))))