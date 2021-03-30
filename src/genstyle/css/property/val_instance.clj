(ns genstyle.css.property.val-instance
  (:require [genstyle.eval :as eval]
            [genstyle.util :as u]
            [genstyle.db :as db])
  )


(def attr->generator-types
  {"color"           [:css/rgba :css/hsla]
   "background"      [:css/rgba :css/hsla]
   "padding"         [:css/padding]
   "margin"          [:css/margin]

   "font-family"     [:css/font-family]
   "font-size"       [:css/pixel-value]
   "display"         [:css/display]
   "flex-direction"  [:css/flex-direction]
   "flex-grow"       [:css/flex-grow]
   "justify-content" [:css/justify-content]
   "align-items"     [:css/align-items]

   "border-color"    [:css/rgba]
   ;"border-width"    [:css/pixel-value]
   "border-width"    [:css/border-width]
   "border-radius"   [:css/pixel-value]
   "border-style"    [:css/border-style]
   })

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
   ::mutf   '(fn
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

(defmethod gen-instance* :css/hsla
  [_]
  {::kind   :css/rgba
   ::valuef '(fn [state]
               (u/mustache-replace
                 "hsla({{h}},{{s}}%, {{l}}%, {{a}})"
                 state))
   ::state  (eval/eval-form
              '{:h               (rand-int 361)
                :s               (rand-int 101)
                :l               (rand-int 101)
                :a               (u/round 1 (rand))
                :mutation-factor (/ (rand) 10)})
   ::mutf   '(fn
               [{:as state :keys [mutation-factor]}]
               (letfn [(mutate [num]
                         (u/bound
                           (int
                             (+ num
                                (* (u/random-flip-sign mutation-factor)
                                   num)))
                           0 255))]
                 (u/fn-map->transform
                   {:h mutate
                    :s mutate
                    :l mutate
                    :a #(u/bound
                          (u/round 1
                                   (+
                                     %
                                     (* (u/random-flip-sign mutation-factor)
                                        %)))
                          0.1 1)}
                   state)))})

(defmethod gen-instance* :css/font-family
  [_]
  {::state (eval/eval-form
             '(rand-nth ["Georgia" "Future" "Calibri" "Times New Roman" "Helvetica"]))})

(defmethod gen-instance* :css/display
  [_]
  {::state (eval/eval-form
             '(rand-nth ["flex" "block" "inline-block" "fixed" "sticky"]))})

(defmethod gen-instance* :css/flex-direction
  [_]
  {::state (eval/eval-form
             '(rand-nth ["row" "column"]))})

(defmethod gen-instance* :css/justify-content
  [_]
  {::state (eval/eval-form
             '(rand-nth ["start" "end" "center" "space-around" "space-between"]))})

(defmethod gen-instance* :css/align-items
  [_]
  {::state (eval/eval-form
             '(rand-nth ["start" "end" "center"]))})

(defmethod gen-instance* :css/border-style
  [_]
  {::state (eval/eval-form
             '(rand-nth ["solid" "dotted" "double" "ridge" "groove"]))})

(defmethod gen-instance* :css/border-width
  [_]
  {::valuef '(fn [state]
               (u/mustache-replace
                 "{{number}}px"
                 state))
   ::state  (eval/eval-form
              '{:number (rand-int 10)})})

(defmethod gen-instance* :css/flex-grow
  [_]
  {#_#_::valuef '(fn [state]
                   (u/mustache-replace
                     "{{number}}px"
                     state))
   ::state (eval/eval-form
             '{:number (rand-int 10)})})

(def ^:private css-pixel-value
  {::kind   :css/pixel-value
   ::valuef '(fn [state]
               (u/mustache-replace
                 "{{number}}px"
                 state))
   ::state  (eval/eval-form
              '{:number          (rand-int 40)
                :mutation-factor (/ (rand) 10)})
   ::mutf   '(fn
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

(defmethod gen-instance* :css/padding
  [_]
  (assoc css-pixel-value ::kind :css/padding))

(defmethod gen-instance* :css/margin
  [_]
  (assoc css-pixel-value ::kind :css/margin))

(defmethod gen-instance* :css/pixel-value
  [_]
  css-pixel-value)

(defn set-value [{:as    instance
                  ::keys [state valuef]
                  :or    {valuef 'identity}}]
  (assoc instance ::value (eval/eval-form (list valuef state))))

(defn make [opts]
  ;(println opts)
  (-> (gen-instance* opts)
      (assoc ::iteration 1)
      set-value
      db/with-timestamps)
  )

(defn clone [val-instance]
  ;(println opts)
  (-> (into {} val-instance)
      (dissoc :created-at)
      (assoc ::iteration 1)
      db/with-timestamps)
  )

(comment
  (make {:property "color"})
  (let [d (make {:property "padding"})]
    (println (::value d))
    (println (::value (mutate d)))))

(defn mutate [{:as instance ::keys [state mutf valuef]}]
  (-> instance
      (update ::state #(eval/eval-form (list mutf %)))
      (update ::iteration inc)
      set-value
      db/with-timestamps)
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
