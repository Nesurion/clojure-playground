(ns clojure-playground.app.main
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [cljs.core.async :refer [<!]]
            [clojure-playground.app.io :as io]))

(enable-console-print!)

(def current-title (r/atom "hello"))

(defn header [s]
  [:div.row.mt-2.mb-3
   [:div.col
    [:h1 s]]])

(defn event-value [event]
  (.-value (.-target event)))


(defn bootstrap-input [title html-attr]
  (let [id (gensym (str title "-"))]
    [:div.form-group
     [:label {:for id} title]
     [:input.form-control (assoc html-attr :id id)]])
  )


(defn bootstrap-textarea [title html-attr]
  (let [id (gensym (str title "-"))]
    [:div.form-group
     [:label {:for id} title]
     [:textarea.form-control (assoc html-attr :id id)]])
  )


(defn new-recipe []
  (let [title (atom "")
        description (atom "")]
    [:div.row
     [:div.col
      [:div.card
       [:div.card-block
        [:h4.card-title "Add recipe"]
        [:form
         [bootstrap-input "Title"
          {:type        "text"
           :placeholder "Enter title"
           :on-change   (fn [event]
                          (reset! title (event-value event)))}]
         [bootstrap-textarea "Description"
          {:type        "text"
           :placeholder "Enter description"
           :on-change   (fn [event]
                          (reset! description (event-value event)))}]
         [:button.btn.btn-primary
          {:type     "button"
           :on-click (fn [event]
                       (go (<! (io/store-recipe {:title       @title
                                                 :description @description}))
                           (io/reset-recipes))
                       false)}
          "add"]]]]]]))


(defn show-recipe-in-accordion [recipe accordion-id]
  (let [card-id (gensym "card")]
    [:div.card
     [:div.card-header {:role "tab"}
      [:h5.mb-0
       [:a {:data-toggle "collapse"
            :data-parent (str "#" accordion-id)
            :href        (str "#" card-id)}
        (str (:title recipe) (:ui-state recipe "X"))]]]
     [:div.collapse {:role "tabpanel"
                     :id   card-id}
      (if (= (:ui-state recipe) :edit)
        [:div.card-block
         [:textarea.form-control (:description recipe)]
         [:button.btn.btn-default
          {:type     "button"
           :on-click (fn [event]
                       (swap! io/recipes
                              (fn [recipes]
                                (assoc-in recipes
                                          [(keyword (:id recipe)) :ui-state] :show))))}
          "cancel"]]
        [:div.card-block
         {:on-click (fn [event]
                      (swap! io/recipes
                             (fn [recipes]
                               (assoc-in recipes
                                         [(keyword (:id recipe)) :ui-state] :edit))))}
         (:description recipe)])
      ]]))


(defn recipes-accordion [recipes]
  [:div.row.mb-4
   [:div.col
    (let [id (gensym "accordion-")]
      [:div {:role "tablist"
             :id   id}
       (for [recipe recipes]
         [show-recipe-in-accordion recipe id])])]])

;; As a user I want to edit a recipe that I've added previously
(defn hello []
  [:div
   [header "cookbook"]
   [recipes-accordion (sort-by :title (vals @io/recipes))]
   [new-recipe]])


(defn ^:export run []
  (io/reset-recipes)
  (r/render [hello]
            (js/document.getElementById "app")))