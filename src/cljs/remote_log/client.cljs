(ns remote-log.client
  (:require [reagent.core :as reagent :refer [atom]]
            [ajax.core :refer [GET POST json-request-format]]
            [taoensso.sente :as sente]
            [clojure.string :as string]))

(enable-console-print!)

(defonce state (atom {:loading false
                      :entries []
                      :user    "admin"
                      :pass    "admin"
                      :token   nil
                      :app     nil
                      :apps    []
                      :timeout nil}))

(defn assoc-state [k v]
  (swap! state assoc k v))

(defn headers []
  {"Authorization" (str "Token " (:token @state))})

(defn acquire-token [cont]
  (POST "acquire-token" {:params        (select-keys @state [:user :pass])
                         :format        (json-request-format)
                         :handler       (fn [{:strs [token]}]
                                          (assoc-state :token token)
                                          (cont))
                         :error-handler (fn [{:keys [response]}]
                                          (assoc-state :token nil)
                                          (js/alert response))}))

(defn load-apps [cont]
  (assoc-state :loading true)
  (GET "apps" {:headers (headers)
               :handler (fn [response]
                          (assoc-state :apps response)
                          (assoc-state :app (first response))
                          (assoc-state :loading false)
                          (cont))}))

(defn load-entries-do
  ([] (load-entries-do :before))
  ([direction] (load-entries-do direction (if (= :before direction)
                                            [first #(into %2 %1)]
                                            [last #(into %1 %2)])))
  ([direction [pick insert]]
   (assoc-state :loading true)
   (GET "entries" {:params        (merge (select-keys @state [:app])
                                         {direction (-> @state :entries pick (get "received"))})
                   :headers       (headers)
                   :handler       (fn [response]
                                    (swap! state update :entries insert response)
                                    (assoc-state :loading false))
                   :error-handler (fn [{:keys [status response]}]
                                    (if (= 401 status)
                                      (acquire-token #(load-entries-do direction))
                                      (do (assoc-state :loading false)
                                          (js/alert response))))})))

(defn load-entries [& args]
  (when (:timeout @state)
    (js/window.clearTimeout (:timeout @state))
    (assoc-state :timeout nil))
  (if (:loading @state)
    (assoc-state :timeout (js/window.setTimeout #(apply load-entries args) 100))
    (apply load-entries-do args)))

(defn listen []
  (let [url (str js/location.pathname "chsk?token=" (:token @state))
        {:keys [ch-recv]} (sente/make-channel-socket! url)
        handler (fn [{:keys [id ?data]}]
                  (when (= id :chsk/recv)
                    (let [[event data] ?data]
                      (when (and (= event :remote-log/push-app)
                                 (= data (:app @state)))
                        (load-entries :after)))))]
    (sente/start-chsk-router! ch-recv handler)))

(defn reload-entries []
  (assoc-state :entries [])
  (load-entries))

(defn maps->table [[m :as ms] date-ks class-k]
  (let [class (fn [k] {:class (str k "-col")})
        fmt (fn [k v] (cond (date-ks k) (-> v js/Date. .toLocaleString)
                            :else v))]
    [:table
     [:thead [:tr
              (map (fn [k] [:th (class k) (string/replace k "-" " ")])
                   (keys m))]]
     [:tbody (map (fn [row] [:tr {:class (str (row class-k) "-row")}
                             (map (fn [[k v]] [:td (class k) (fmt k v)])
                                  row)])
                  ms)]]))

(defn input [k]
  [:input {:type        :text
           :value       (k @state)
           :placeholder (name k)
           :on-change   #(assoc-state k (-> % .-target .-value))}])

(defn select [val opts f]
  [:select {:value     (val @state)
            :on-change (fn [e] (assoc-state val (-> e .-target .-value)) (f))}
   (map #(vector :option %) (opts @state))])

(defn button [f enabled text]
  [:button (merge {:on-click #(f)} (when-not enabled {:disabled :disabled})) text])

(defn page []
  (if (:token @state)
    [:div
     [:div.header
      (button load-entries (not (:loading @state)) "<< Load previous")
      (select :app :apps reload-entries)
      (if (:loading @state)
        [:div.loading "Loading..."])]
     (maps->table (:entries @state) #{"received-at", "occurred-at"} "level")]
    [:form {:action "javascript:"}
     (input :user)
     (input :pass)
     (button #(acquire-token (fn [] (load-apps load-entries) (listen))) true "Go")]))

(reagent/render-component [page] (js/document.getElementById "app"))
