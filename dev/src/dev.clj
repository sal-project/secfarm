(ns dev
  (:require [com.stuartsierra.component :as component]
            [com.stuartsierra.component.repl :refer [reset set-init start stop system]]
            [clojure.tools.namespace.repl :refer [refresh]]
            [ragtime.jdbc :as rjdbc]
            [ragtime.repl :as rrepl]
            [clojure.java.jdbc :as jdbc]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [hikari-cp.core]
            [secfarm.core]))

(clojure.tools.namespace.repl/set-refresh-dirs "dev/src" "src" "test")

(set-init secfarm.core/init-system)

(defn load-db-config []
  (let [{{data :datasource-option} :db} (edn/read-string (slurp (io/resource "config.edn")))]
    {:dbtype (:adapter data)
     :dbname (:database-name data)
     :host (:server-name data)
     :port (:port-number data)
     :user (:username data)
     :password (:password data)}))

(defn today-sql-date []
  (java.sql.Date. (System/currentTimeMillis)))

(def migration-config
  {:datastore (rjdbc/sql-database (load-db-config))
   :migrations (rjdbc/load-resources "migrations")})

(defn migrate []
  (rrepl/migrate migration-config))

(defn rollback []
  (rrepl/rollback migration-config))

