(ns secfarm.db
  (:require [hikari-cp.core :refer :all]
            [com.stuartsierra.component :as component]))

(defrecord Db [datasource config]
  component/Lifecycle
  (start [this]
    (assoc this :datasource (delay (make-datasource
                                    (-> (:option config)
                                        :db
                                        :datasource-option)))))
  
  (stop [this]
    (let [datasource (:datasource this)]
      (close-datasource @datasource))
    (assoc this :datasource nil)))
