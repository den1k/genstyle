(ns genstyle
  (:require [malli.core :as m]
            [medley.core :as md]
            [genstyle.util :as u]
            [genstyle.db :as db]
            [genstyle.css.stylesheet :as stylesheet]

            [genstyle.project :as project]
            [mount.core :as mount :refer [defstate]]
            [org.httpkit.server :as httpkit]
            [hiccup.core :as h]
            [etaoin.api :as et]
            [genstyle.generation :as generation]
            [genstyle.style :as style]
            [genstyle.css.ruleset :as ruleset]
            [genstyle.css.declaration :as decl]
            [uix.dom.alpha :as uix.dom]
            [ring.util.response :as resp]
            [reitit.ring :as r.ring]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [ring.middleware.resource :refer [wrap-resource]]
            [reitit.ring.middleware.parameters :as r.ring.params]
            [genstyle.web.landing :as landing]
            [net.cgrand.xforms :as x]))

;(mount/start)
;(def driver (et/chrome))


;(et/go driver "https://google.com")
;(mount/start)


;; from a gen-spec of css classes, with attrs and value ranges
;; gen a population of styles

(defn make-css-project! [opts]
  (some-> (project/make-css-project opts)
          vector
          db/transact!))

(defn mate-css-project! [opts]
  (some-> (project/mate-css-project opts)
          vector
          db/transact!))

(comment
  (make-css-project! {:name            "test-project"
                      :selector->props {".header" ["color"]
                                        ".text"   ["padding"]}})

  (db/transact! [[:db/retractEntity [::project/name "genstyle-landing"]]])

  (db/transact! [{:db/id 2829 ::style/fitness 5}])

  (make-css-project!
    {:name            "genstyle-landing"
     :selector->props {".landing"                   ["display" "flex-direction" "justify-content" "align-items"]
                       ".main-section"              ["padding" "background" "display" "flex-grow"]
                       ".main-headers"              ["display" "font-family"]
                       ;".main-headers .header"      []
                       ;".main-headers .subheader"   []
                       ".main-headers .premise"     ["margin"]
                       ".main-headers .subheader"   ["font-family" "margin"]
                       ".endorsement-section"       ["padding" "background" "display" "flex-direction" "flex-grow"]
                       ".endorsement"               ["border-radius" "border-width" "border-color" "border-style" "font-family" "padding"]
                       ".endorsement .quote"        ["font-size"]
                       ".endorsement .quote-author" ["font-size"]
                       ".pricing-section"           ["padding" "background" "display" "flex-direction" "flex-grow"]
                       ".pricing-header"            ["display" "justify-content" "font-family"]
                       ".pricing-tiers"             ["display" "justify-content" "align-items"]
                       ".pricing-tier"              ["border-color" "border-width" "border-radius"
                                                     "border-style" "padding" "margin"
                                                     #_"drop-shadow" "display" "flex-direction"
                                                     "justify-content" "align-items"]
                       ".pricing-tier .tier-name"   ["font-size" "font-family"]

                       }})

  (mate-css-project! {:name            "genstyle-landing"
                      :generation-name "GEN_Triumphant-Tyler"}))

(defn style->route [style-ent]
  (let [gen (style/ent->gen style-ent)]
    (str "/project/"
         (generation/ent->project-name gen)
         "/population/style/"
         (:db/id style-ent)))
  )

(defn thumbnail-style-hiccup [{:as style :keys [parents] ::style/keys [fitness]}]
  (let [route (style->route style)]
    [:div.flex.flex-column.ba.pa1
     {:style {:flex-basis 0}}
     [:div.pb1.f4.i
      {:style {:margin      0
               :font-family "monospace"}}
      [:div [:span.b.pr2 "EID:"] (:db/id style)]
      [:div [:span.b.pr2 "FITNESS:"] fitness]
      (when parents
        (into [:div.flex.flex-wrap [:span.b.pr2 "PARENTS: "]]
              (comp (map (fn [s] [:a.black {:href (style->route s)} (:db/id s)]))
                    (interpose ", "))
              parents))
      #_[:div.f6.flex.justify-between
         [:div
          [:a.b.link {:href (str "/api/style/" (:db/id style) "/inc")} "+"]
          "/"
          [:span.b "-"]]]
      [:a.f6.i {:href route} "LINK"]]
     [:iframe
      {:style  {:outline "none"
                :border  "none"}
       :width  200
       :height 250
       :src    (str route "?thumbnail=1")}]])
  )


(defn generation-styles-hiccup [{:as gen ::generation/keys [styles name]}]
  [:div.mb5
   [:div.mb3
    [:a.link.black
     {:href (str "/project/"
                 (generation/ent->project-name gen)
                 "/generation/"
                 name)}
     [:h1.i.sans-serif.dim {:style {:font-size 50
                                    :margin    0}}
      name]]]
   [:div.f6.i.pl4.sans-serif.self-end "BORN: " (:created-at gen)]
   [:div.flex.flex-wrap.pl4
    (for [{:as style ::style/keys [fitness]}
          (sort-by ::style/fitness > styles)]
      [thumbnail-style-hiccup style])]])

(defn project-hiccup [project-name]
  (let [gens (project/project-generations-desc project-name)]
    [:div.flex.flex-wrap
     (map generation-styles-hiccup gens)]))


(defn style-hiccup [{:as p :keys [style-id thumbnail]}]
  (let [style (db/entity style-id)]
    [:html
     [:meta {:charset "UTF-8"}]
     [:head
      [:style
       {:data-style-id           (:db/id style)
        :data-project            (style/ent->project-name style)
        :data-generation         (style/ent->gen-name style)
        :dangerouslySetInnerHTML {:__html (stylesheet/from-style style)}}]]
     [:body
      (when thumbnail {:style {:zoom :30%}})
      [landing/view]]]))

(defn html-response [html]
  {:status  200
   :headers {"content-type" "text/html"}
   :body    html})

(defn hiccup-response [body-html]
  (html-response
    (uix.dom/render-to-static-markup
      [:html
       [:meta {:charset "UTF-8"}]
       [:head
        [:link {:rel "stylesheet" :href "/css/tachyons.css"}]]
       [:body body-html]])))

(def router
  (r.ring/router
    [["/project/:proj-name"
      [""
       {:get (fn [{:as req :keys [path-params]}]
               (hiccup-response (project-hiccup (:proj-name path-params))))}]

      ["/generation/:generation-name"
       {:get (fn [{:as req {:keys [generation-name proj-name]} :path-params}]
               (let [gen (if (= "latest" generation-name)
                           (project/project-latest-generation proj-name)
                           (db/entity [::generation/name generation-name]))]
                 (some-> gen
                         generation-styles-hiccup
                         hiccup-response)))}]
      ["/population/alphas"
       {:get (fn [{:as req :keys [path-params query-params]}]
               (let [alphas (project/alpha-styles (:proj-name path-params))]
                 (hiccup-response [:div.flex.flex-wrap
                                   (map thumbnail-style-hiccup alphas)])))}]
      ["/population/style/:style-id"
       {:get (fn [{:as req :keys [path-params query-params]}]
               (let [params (merge
                              (md/map-keys keyword query-params)
                              (update path-params :style-id #(Integer/parseInt %)))]
                 (hiccup-response (style-hiccup params))))}
       ]]]))

(defn resource-handler [handler]
  (-> handler
      (wrap-resource "public")
      (wrap-content-type)
      (wrap-not-modified)))

(def handler
  (r.ring/ring-handler
    router
    (r.ring/create-default-handler)
    {:middleware [r.ring.params/parameters-middleware
                  resource-handler]}))

(defstate server
  :start (httpkit/run-server
           handler
           {:port                 4455
            :legacy-return-value? false})
  :stop (httpkit/server-stop! server)
  )

;(mount/start)





(defn make-styles-generation [{:keys [n class->attrs]
                               :or   {n 5}}])

(defn order-fittest-desc [styles])

(defn make-child
  "Makes a child-style from a coll of 2 or more styles"
  [styles])

(defn next-generation [styles])

