(ns genstyle.css.property.val-instance
  (:require [genstyle.eval :as eval]
            [genstyle.util :as u])
  )

(def attr->generator-types
  {"color"   [:css/rgba]
   "padding" [:css/padding]})

(defn attr->rand-generator-type [property]
  (some-> (get attr->generator-types property)
          rand-nth))

(defmulti gen-instance*
  (fn [{:keys [property generator-type]}]
    (or
      generator-type
      (attr->rand-generator-type property))))

(defmethod gen-instance* :css/rgba
  [_]
  {::kind   :css/rgba
   ::valuef '(fn [state]
               (u/mustache-replace
                 "rgba({{r}},{{g}}, {{b}}, {{a}})"
                 state))
   ::state  (eval/eval-form
              '{:r               (rand-int 256)
                :g               (rand-int 256)
                :b               (rand-int 256)
                :a               (u/round 1 (rand))
                :mutation-factor (/ (rand) 10)})
   ::f      '(fn
               [{:as state :keys [mutation-factor]}]
               (letfn [(mutate [num]
                         (u/bound
                           (int
                             (+ num
                                (* (u/random-flip-sign mutation-factor)
                                   num)))
                           0 255))]
                 (u/fn-map->transform
                   {:r mutate
                    :g mutate
                    :b mutate
                    :a #(u/bound
                          (u/round 1
                                   (+
                                     %
                                     (* (u/random-flip-sign mutation-factor)
                                        %)))
                          0.1 1)}
                   state)))})

(defmethod gen-instance* :css/padding
  [_]
  {::kind   :css/padding
   ::valuef '(fn [state]
               (u/mustache-replace
                 "{{number}}px"
                 state))
   ::state  (eval/eval-form
              '{:number          (rand-int 40)
                :mutation-factor (/ (rand) 10)})
   ::f      '(fn
               [{:as state :keys [mutation-factor]}]
               (letfn [(mutate [num]
                         (u/bound
                           (int
                             (+ num
                                (* (u/random-flip-sign mutation-factor)
                                   num)))
                           0 40))]
                 (u/fn-map->transform
                   {:number mutate}
                   state)))})

(defn set-value [{:as instance ::keys [state valuef]}]
  (assoc instance ::value (eval/eval-form (list valuef state))))

(defn make [opts]
  (-> (gen-instance* opts)
      (update ::iteration (fnil inc 0))
      set-value)

  )



(comment
  (make {:property "color"})
  (let [d (make {:property "padding"})]
    (println (::value d))
    (println (::value (mutate d)))))

(defn mutate [{:as instance ::keys [state f valuef]}]
  (-> instance
      (update ::state #(eval/eval-form (list f %)))
      (update ::iteration inc)
      set-value)
  )


(comment
  (def c (make {:property "color"}))
  (-> c
      mutate
      mutate
      mutate
      mutate
      mutate
      ::value
      ))
