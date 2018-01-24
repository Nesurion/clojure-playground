(ns cookbook.app.main
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [cljs.core.async :refer [<!]]
            [markdown.core :refer [md->html]]
            [cookbook.app.io :as io]))

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


(defn button
  ([label on-click-fn]
   (button label on-click-fn "btn-default"))
  ([label on-click-fn class]
   [:button.btn
    {:class    class
     :type     "button"
     :on-click on-click-fn}
    label]))


(defn render-ingredients [ingredients]
  [:ul {:id "ingredients"}
   (for [x @ingredients]
     [:li x])])


(defn render-ingredient-input [ingredient-input]
  [:input.form-control {:class       "input-sm"
                        :value       @ingredient-input
                        :type        "text"
                        :placeholder "Add ingredient"
                        :on-change   (fn [event]
                                       (reset! ingredient-input (event-value event)))}])


(defn new-recipe []
  (let [title (atom "")
        description (atom "")
        ingredient-input (r/atom "")
        ingredients (r/atom [])]
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
         [:div.form-group
          [:label "Ingredients"]
          [render-ingredients ingredients]]
         [:div.input-group
          [render-ingredient-input ingredient-input]
          [:span.input-group-btn
           [button "add" (fn [event]
                           (swap! ingredients conj @ingredient-input)
                           (reset! ingredient-input "")
                           false)
            "btn-secondary"]]]
         [bootstrap-textarea "Description"
          {:type        "text"
           :placeholder "Enter description"
           :on-change   (fn [event]
                          (reset! description (event-value event)))}]
         [button "add" (fn [event]
                         (go (<! (io/store-recipe {:title       @title
                                                   :ingredients @ingredients
                                                   :description @description}))
                             (io/reset-recipes))
                         false)
          "btn-primary"]]]]]]))


(defn render-ingredients-show [ingredients]
  [:ul
    (for [x ingredients]
      [:li x])])

(defn render-recipe-edit [recipe]
  (let [description-edit (atom (:description recipe))]
    [:div.card-block
     [render-ingredients-show (:ingredients recipe)]
     [:textarea.form-control
      {:on-change (fn [event]
                    (reset! description-edit (event-value event)))}
      (:description recipe)]
     [button "cancel"
      (fn [event]
        (swap! io/recipes
               (fn [recipes]
                 (assoc-in recipes
                           [(keyword (:id recipe)) :ui-state]
                           :show))))]
     [button "save" (fn [event]
                      (go (<! (io/store-recipe {:id          (:id recipe)
                                                :description @description-edit}))
                          (io/reset-recipes))
                      false)
      "btn-primary"]]))


(defn render-recipe-show [recipe]
  [:div.card-block
   [render-ingredients-show (:ingredients recipe)] 
   [:div {:dangerouslySetInnerHTML  {:__html (md->html (:description recipe))}
          :on-click (fn [event]
                (swap! io/recipes
                       (fn [recipes]
                         (assoc-in recipes
                                   [(keyword (:id recipe)) :ui-state]
                                   :edit))))}
    ]])


(defn show-recipe-in-accordion [recipe accordion-id]
  (let [card-id (gensym "card")]
    [:div.card
     [:div.card-header {:role "tab"}
      [:h5.mb-0.display_inline
       [:a {:data-toggle "collapse"
            :data-parent (str "#" accordion-id)
            :href        (str "#" card-id)}
        (str (:title recipe))]]
      [button "x" (fn [event]
                    (go (<! (io/delete-recipe (:id recipe)))
                        (io/reset-recipes))
                    false)
       "close"]]
     [:div.collapse {:role "tabpanel"
                     :id   card-id}
      (if (= (:ui-state recipe) :edit)
        (render-recipe-edit recipe)
        (render-recipe-show recipe))]]))


(defn recipes-accordion [recipes]
  [:div.row.mb-4
   [:div.col
    (let [id (gensym "accordion-")]
      [:div {:role "tablist"
             :id   id}
       (for [recipe recipes]
         [show-recipe-in-accordion recipe id])])]])

(defn debug [label state-atom]
  [:div
   [:h6 label]
   [:code
    (prn-str state-atom)]])

(defn app []
  [:div
   [header "cookbook"]
   [recipes-accordion (sort-by :title (vals @io/recipes))]
   [new-recipe]])


(defn ^:export run []
  (io/reset-recipes)
  (r/render [app]
            (js/document.getElementById "app")))
