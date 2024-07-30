(ns user
  (:require [com.stuartsierra.component.user-helpers
             :refer [dev go reset]]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [ragtime.jdbc :as rjdbc]
            [ragtime.repl :as rrepl]))

(defn load-db-config []
  (let [{{data :datasource-option} :db} (edn/read-string (slurp (io/resource "config.edn")))]
    {:dbtype (:adapter data)
     :dbname (:database-name data)
     :host (:server-name data)
     :port (:port-number data)
     :user (:username data)
     :password (:password data)}))

(def migration-config
  {:datastore (rjdbc/sql-database (load-db-config))
   :migrations (rjdbc/load-resources "migrations")})

(defn migrate []
  (rrepl/migrate migration-config))

(defn rollback []
  (rrepl/rollback migration-config))
