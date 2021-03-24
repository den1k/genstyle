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
            [genstyle.style :as style]
            [uix.dom.alpha :as uix.dom]
            [ring.util.response :as resp]
            [reitit.ring :as r.ring]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [ring.middleware.resource :refer [wrap-resource]]))

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

(comment
  (make-css-project! {:name            "test-project"
                      :selector->props {".header" ["color"]
                                        ".text"   ["padding"]}}))


(defn project-hiccup [project-name]
  (let [{:as proj ::project/keys [population]} (db/entity [::project/name project-name])
        gen-name->styles (group-by style/ent->gen-name population)]
    [:document
     [:head
      [:link {:rel "stylesheet" :href "/css/tachyons.css"}]]
     [:body
      [:div.flex.flex-wrap
       (for [[gen-name styles] gen-name->styles]
         [:div.mt4
          [:h1.i.sans-serif {:style {:font-size 50}}
           gen-name]
          [:div.flex.flex-wrap.pl4
           (for [{:as style ::style/keys [fitness]} styles
                 :let [route (str "/project/"
                                  project-name
                                  "/population/style/"
                                  (:db/id style))]]
             [:div.flex.flex-column.ba.pa1
              [:pre
               {:style {:margin 0}}
               [:div.f6.i [:span.b "EID:"] (:db/id style)]
               [:div.f6.i [:span.b "FITNESS:"] fitness]]
              [:iframe
               {:style {:outline "none"
                        :border  "none"}
                :src   route}]])]])]]]
    ))

(defn style-hiccup [style-eid]
  (let [style (db/entity style-eid)]
    [:document
     [:head
      [:style (stylesheet/from-style style)]]
     [:body
      [:div.header "Hi there"]
      [:div.text "Lorem ipsum dolor sit amet, consectetur adipiscing elit."]]]))

(defn html-response [html]
  {:status  200
   :headers {"content-type" "text/html"}
   :body    html})

(defn hiccup-response [hiccup]
  {:status  200
   :headers {"content-type" "text/html"}
   :body    (uix.dom/render-to-static-markup hiccup)})

(def router
  (r.ring/router
    [["/project/:proj-name"
      {:get (fn [{:as req :keys [path-params]}]
              (hiccup-response (project-hiccup (:proj-name path-params))))}
      ]
     ["/project/:proj-name/population/style/:style-id"
      {:get (fn [{:as req :keys [path-params]}]
              (hiccup-response (style-hiccup (Integer/parseInt (:style-id path-params)))))}
      ]]))

(defn resource-handler [handler]
  (-> handler
      (wrap-resource "public")
      (wrap-content-type)
      (wrap-not-modified)))

(def handler
  (r.ring/ring-handler
    router
    (r.ring/create-default-handler)
    {:middleware [resource-handler]}))

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

