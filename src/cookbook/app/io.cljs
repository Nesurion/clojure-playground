(ns cookbook.app.io
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]))

(def recipes (r/atom {}))

(def base-url "http://localhost:3000")

(defn reset-recipes []
  (go (let [url "/recipes"
            response (<! (http/get url
                                   {:with-credentials? false
                                    :query-params      {"since" 135}}))]
        (prn [url (:status response)])
        (let [response-recipes (:recipes (:body response))]
          (reset! recipes response-recipes)
          (prn @recipes)))))

(defn delete-recipe [recipe-id]
  (http/delete "/recipes" {:query-params {:id recipe-id}}))

(defn store-recipe [recipe]
  (http/post "/recipes" {:json-params recipe}))

