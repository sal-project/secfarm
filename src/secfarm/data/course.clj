(ns secfarm.data.course
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [honey.sql.helpers :as h]
            [honey.sql :as sql]
            [secfarm.utils :refer [unqualified]]
            [secfarm.data.utils :as u]))

(defn get-privilege-by-gid [db gid]
  (map unqualified
       (u/execute-sql db
                      (-> (h/select :*)
                          (h/from :coursepriv)
                          (h/where (if (sequential? gid)
                                     [:in :gid gid]
                                     [:= :gid gid]))
                          (sql/format {:pretty true})))))

(defn- privilege-ids-of-gids
  "obtain a set of existing course-id information of privilege for each gid."
  [db gids]
  (let [course-lists (map (fn [gid] ;; TODO: gid毎にget-privilege-by-gidを呼び出す実装を一括で呼び出すように修正したい
                            (map #(get % :course_id)
                                 (get-privilege-by-gid db gid)))
                          gids)
        course-sets (map #(set %) course-lists)]
    (into {} (map vector gids course-sets))))

(defn- priv-info-hash->list [info]
  [(:gid info)
   (:cid info)
   (get info :show false)
   (get info :attend false)
   (get info :execute false)])

(defn- priv-info-hash->update-hash [info]
  (loop [result {}
         keylst (keys info)]
    (if (empty? keylst)
      result
      (let [key (first keylst)]
        (cond
          (and (= :show key) (some? (:show info)))
          (recur (assoc result :show_p (:show info))
                 (rest keylst))
          (and (= :attend key) (some? (:attend info)))
          (recur (assoc result :attend_p (:attend info))
                 (rest keylst))
          (and (= :execute key) (some? (:execute info)))
          (recur (assoc result :execute_p (:execute info))
                 (rest keylst))
          :else (recur result (rest keylst)))))))

(defn set-privilege-of-gid [db gid existing-ids priv-infos]
  (let [existance-group (group-by #(contains? existing-ids (:cid %)) priv-infos)
        update-infos (get existance-group true)
        insert-infos (get existance-group false)]
    (jdbc/with-transaction [tx (deref db)]
      (when-not (empty? insert-infos)
        (jdbc/execute-one! tx
                           (-> (h/insert-into :coursepriv)
                               (h/columns :gid :course_id :show_p :attend_p :execute_p)
                               (h/values (map priv-info-hash->list insert-infos))
                               (sql/format {:pretty true}))))

      (doseq [info update-infos]
        (jdbc/execute-one! tx
                           (-> (h/update :coursepriv)
                               (h/set (priv-info-hash->update-hash info))
                               (h/where [:= :gid (:gid info)]
                                        [:= :course_id (:cid info)])
                               (sql/format {:pretty true})))))))

(defn set-privilege [db priv-info]
  (let [gids (map #(get % :gid) priv-info)
        course-ids (privilege-ids-of-gids db gids)]
    (let [gids (keys course-ids)]
      (doseq [gid gids]
        (set-privilege-of-gid db
                              gid
                              (get course-ids gid)
                              (filter #(= (:gid %) gid) priv-info))))))
