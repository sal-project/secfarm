(ns secfarm.config
  (:require [com.stuartsierra.component :as component]
            [clojure.java.io :as io]
            [clojure.edn :as edn]))

(defrecord Config [config-path]
  component/Lifecycle
  (start [this]
    (assoc this :option (edn/read-string (slurp config-path))))
  (stop [this]
    this) )
