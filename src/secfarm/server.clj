(ns secfarm.server
  (:require [com.stuartsierra.component :as component]
            [ring.adapter.jetty :as jetty]))

(defrecord Server [instance app config]
  component/Lifecycle
  (start [this]
    (cond-> this
      (not instance)
      (assoc :instance (jetty/run-jetty (:handler app) (:server (:option config))))))
  (stop [this]
    (when instance
      (.stop instance))
    (assoc this :instance nil)))
