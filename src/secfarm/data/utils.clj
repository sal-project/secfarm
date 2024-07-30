(ns secfarm.data.utils
  (:require [next.jdbc :as jdbc]
            [honey.sql.helpers :as h]))

(defn execute-one-sql [db & execute-opts]
  (with-open [conn (-> (jdbc/get-datasource (deref db))
                       (jdbc/get-connection))]
    (jdbc/execute-one! conn (first execute-opts))))

(defn execute-sql [db & execute-opts]
  (with-open [conn (-> (jdbc/get-datasource (deref db))
                       (jdbc/get-connection))]
    (jdbc/execute! conn (first execute-opts))))
