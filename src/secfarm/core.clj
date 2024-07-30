(ns secfarm.core
  (:gen-class)
  (:require [com.stuartsierra.component :as component]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [hikari-cp.core :as hikari]
            [secfarm.config :as config]
            [secfarm.lecdata :as lecdata]
            [secfarm.db :as db]
            [secfarm.app :as app]
            [secfarm.server :as server]
            [secfarm.data.user :as user]
            [secfarm.data.lecdata :as dblecdata]))

(defn init-system [& _]
  (component/system-map
   :config (config/map->Config {:config-path (io/resource "config.edn")})
   :lecdata (component/using (lecdata/map->Lecdata {}) [:config])
   :db (component/using (db/map->Db {}) [:config])
   :app (component/using (app/map->App {}) [:config :db :lecdata])
   :server (component/using (server/map->Server {}) [:app :config])))

(def system (atom nil))

(defn start-server []
  (reset! system (component/start (init-system))))

(defn stop-server []
  (reset! system (component/stop (deref system))))

(defn -main [& _]
  (start-server))

(defn- load-db []
  (delay (hikari/make-datasource
          (-> (:db (edn/read-string (slurp (io/resource "config.edn"))))
              :datasource-option))))

(defn initialize [admin-username admin-contact admin-password]
  (let [db (load-db)]
    (if (user/empty-user? db)
      (user/initialize-user-environment db {:username admin-username
                                            :contact admin-contact
                                            :password admin-password})
      (println "Data exists already."))
    (hikari/close-datasource (deref db))))

(defn create-user [username contact password]
  (let [db (load-db)]
    (if-not (user/empty-user? db)
      (let [result (user/add-user
                    db
                    {:username username
                     :contact contact
                     :password password})]
        (hikari/close-datasource (deref db))
        result)
      (do
        (println "You must perform initialize function (secfarm.core/initialize).")
        (hikari/close-datasource (deref db))))))

(defn grouping [groupname username]
  (let [db (load-db)]
    (if-not (user/empty-user? db)
      (if-let [uid (:uid (user/get-user-by-username db username))]
        (user/set-user-group db uid groupname)
        (println (str username " user is not found.")))
      (println "You must perform initialize function (secfarm.core/initialize)."))
    (hikari/close-datasource (deref db))))

(defn ungrouping [groupname username]
  (let [db (load-db)
        uid (:uid (user/get-user-by-username db username))
        gid (:gid (user/get-group-by-groupname db groupname))]
    (when (and uid gid (user/belong? db uid gid))
      (user/unset-user-group db uid gid))
    (hikari/close-datasource (deref db))))

(defn reset-user-progress []
  (let [db (load-db)]
    (dblecdata/reset-all-progress db)))
