(ns cookbook.app.components)


(defn tabs
  "Each tab-defs should be a seq of maps with :title and :link."
  [tab-defs active-tab-title]
  [:ul.nav.nav-tabs
   (for [{:keys [title link on-click]} tab-defs]
     [:li.nav-item
      [:a.nav-link {:href link
                    :on-click on-click
                    :class (when (= title active-tab-title) "active")} 
       title]])])

(defn list-group
  [things]
  [:ul.list-group
   (for [thing things]
     [:li.list-group-item thing])])

(defn icon
  [icon-name]
  [:span.oi {:data-glyph icon-name
             :title icon-name
             :aria-hidden "true"}])


(defn outline-button
  [btn-body & [{:keys [btn-type on-click]
                :or {btn-type "primary"}
                :as html-attributes}]]
  (println html-attributes)
  [:button {:type "button"
            :on-click on-click
            :class (str "btn btn-outline-" btn-type)}
   btn-body])

(defn badge
  [body & [{:keys [color] :or {color "primary"}}]]
  [:span {:class (str "badge badge-" color)} body])
