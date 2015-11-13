(ns nekoban.core
  (:require-macros [secretary.core :refer [defroute]]
                   [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [sablono.core :as h :refer-macros [html]]
            [secretary.core :as secretary :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [cljs.core.async :refer [put! chan <! pub sub]])
  (:import goog.History))

(enable-console-print!)

(defonce app-state (atom {:text "Hello Chestnut!"}))

(def pub-chan (chan))
(def sub-chan (pub pub-chan :topic))

(defn consume-events
  "Consume events from the pub chan.
  topic is a keyword indicating the topic to subscribe to
  func takes an arg (the event)
  owner is an Om component"
  [owner topic func]
  (let [sub-chan (om/get-shared owner :sub-chan)]
    (let [event-chan (sub sub-chan topic (chan))]
      (go-loop [event (<! event-chan)]
               (func event)
               (recur (<! event-chan))))))

(defn change-view
  ([view view-name]
   (put! pub-chan {:topic :change-view
                   :view view
                   :view-name view-name}))
  ([view view-name init-state]
   (put! pub-chan {:topic :change-view
                   :view view
                   :view-name view-name
                   :view-init-state init-state})))

; routes
(declare home-view)
(declare about-view)

(defroute home-path "/" []
  (change-view home-view :home-view))

(defroute about-path "/about" []
  (change-view about-view :about-view))

; React components
(defcomponent home-view [app owner]
  (render [_]
          (html [:p "Nekoban Home"])))

(defcomponent about-view [app owner]
  (render [_]
          (html [:p "About Nekoban"])))

(defn menu [& content]
  [:span [:ul
          [:li [:a {:href (home-path)} "Home"]]
          [:li [:a {:href (about-path)} "About Nekoban"]]]
   [:hr]
   [:div.main-section content]
   [:hr]
   [:div.footer
    [:p "Copyright 2015 Nekoban"]]])

(defcomponent root-view [app owner]
  (init-state [_]
              (:view home-view))
  (did-mount [_]
             (consume-events owner :change-view
                             (fn [{:keys [view view-init-state view-name]}]
                               (om/set-state! owner :view view)
                               (om/set-state! owner :view-init-state view-init-state)
                               (om/set-state! owner :react-key view-name))))
  (render-state [_ {:keys [view view-init-state react-key]}]
                (html
                  (menu
                    (om/build view app {:init-state view-init-state :react-key react-key})))))

; configuration

(defn main[]
  (om/root
    root-view
    app-state
    {:target (. js/document (getElementById "app"))
     :shared {:sub-chan sub-chan
              :pub-chan pub-chan}}))

; Hack for browsers without HTML5 history
(secretary/set-config! :prefix "#")

; Configure secretary to listen for navigation events
(let [h (History.)]
  (goog.events/listen h EventType/NAVIGATE #(secretary/dispatch! (.-token %)))
  (doto h (.setEnabled true)))
