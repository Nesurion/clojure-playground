(ns cookbook.core
  (:require
    [clojure.string :as string :refer [split]]
    [clojure.edn :as edn]
    [ring.adapter.jetty :as jetty :refer [run-jetty]]
    [ring.middleware.params :refer [wrap-params]]
    [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
    [mount.core :as mount :refer [defstate]]
    [compojure.core :refer [defroutes GET POST OPTIONS DELETE]]
    [compojure.route :as route]
    [cheshire.core :as json]
    [ring.util.response :as resp]
    [clojure.tools.logging :as log]
    [ring.middleware.gzip :refer [wrap-gzip]]
    [environ.core :refer [env]]
    [ring.middleware.basic-authentication :refer [wrap-basic-authentication]])
  (:import [java.util UUID])
  (:gen-class))

(def basic-auth-user     (:basic-auth-user env))
(def basic-auth-password (:basic-auth-password env))

(defn slurp-edn [filename default-value]
  (try
    (log/info "reading state from" filename)
    (edn/read-string (slurp filename))
    (catch java.io.FileNotFoundException e
      (log/error "file not found" filename ", falling back to" default-value)
      default-value)))


(def recipes-filename "data/recipes.edn")
(def recipes (atom (slurp-edn recipes-filename {})))
(add-watch recipes :recipes-file-store
           (fn [_ _ _ new-value]
             (log/info "writing new recipe to" recipes-filename)
             (spit recipes-filename (prn-str new-value))))


(defn authenticated? [name pass]
  (and (= name basic-auth-user)
       ;; don't store the password in plain text
       (= pass basic-auth-password)))


(defn access-controll-headers
  [handler]
  (fn [req]
    (let [response (handler req)]
      (update-in response
                 [:headers]
                 merge
                 {"Access-Control-Allow-Origin" "*"
                  "Access-Control-Allow-Headers"
                                                ["Content-Type"
                                                 "Authorization"
                                                 "Access-Control-Allow-Origin"]}))))

(defn convert-to-uuid [uuid-str]
  (if (string? uuid-str)
    (UUID/fromString uuid-str)
    uuid-str))


(defn add-recipe [recipe]
  (swap! recipes
         (fn [current-recipes]
           (let [recipe-with-id (merge {:id (UUID/randomUUID)}
                                       recipe)]
             (merge-with merge current-recipes
                         {(convert-to-uuid (:id recipe-with-id)) recipe-with-id})))))


(defn remove-recipe [id]
  (swap! recipes (fn [current-recipes]
                   (dissoc current-recipes id))))


(defroutes routes
           (GET "/recipes" []
             {:status 200
              :body   {:recipes (into {} (for [[id recipe] @recipes]
                                           [id (assoc recipe :id id)]))}})
           (DELETE "/recipes" request
             (let [id (UUID/fromString (get-in request [:params "id"]))]
               (if (contains? @recipes id)
                 (do
                   (remove-recipe id)
                   {:status 200})
                 {:status 404}))
             )
           (GET "/" [] (resp/resource-response "/index.html" {:root "public"}))
           (POST "/recipes" request (do
                                      (add-recipe (get request :body))
                                      {:status 200
                                       :body   {:type :okay}}))
           (route/resources "/")
           (route/not-found "<h1>Page not found</h1>"))


(def http-pipeline (-> routes
                       wrap-params
                       wrap-json-response
                       (wrap-json-body {:keywords? true})
                       access-controll-headers
                       wrap-gzip
                       (wrap-basic-authentication authenticated?)))

(defstate server :start (run-jetty #'http-pipeline {:port  3000
                                                    :join? false})
          :stop (.stop server))

(defn config-check!
  "Checks that all mandatory config is set.
  System exits otherwise."
  []
  (when (nil? basic-auth-user)
    (log/error "Basic auth user not set! Please set BASIC_AUTH_USER")
    (mount/stop)
    (System/exit 1))

  (when (nil? basic-auth-password)
    (log/error "Basic auth password not set! Please set BASIC_AUTH_PASSWORD")
    (mount/stop)
    (System/exit 2)))

(defn restart []
  (config-check!)
  (mount/stop)
  (mount/start))


(defn -main [& args]
  (config-check!)
  (mount/start))


(comment
  (restart))

