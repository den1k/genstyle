(ns genstyle.css.declaration
  (:require [genstyle.css.property.val-instance :as pinst]))

(defn make [prop]
  {::prop         prop
   ::val-instance (pinst/make {:property prop})})

(defn clone [{::keys [prop val-instance]}]
  {::prop         prop
   ::val-instance (pinst/clone val-instance)})
