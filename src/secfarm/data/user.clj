(ns secfarm.data.user
  (:require [buddy.hashers :as hashers]
            [clojure.set :as cset]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [honey.sql.helpers :as h]
            [honey.sql :as sql]
            [secfarm.common :refer [admin-group-name
                                    default-group-name]]
            [secfarm.utils :refer [unqualified]]
            [secfarm.data.utils :as u]))

(defn hash-password [password-string]
  (hashers/derive password-string {:alg :pbkdf2+blake2b-512}))

(defn replace-password->passwdhash [h]
  (if-let [password-string (:password h)]
    (dissoc (assoc h :passwdinfo (hash-password password-string)) :password)
    h))

(defn empty-user? [db]
  (every? #(= % 0)
          (map (fn [dbname]
                 (:count
                  (u/execute-one-sql db
                                     (-> (h/select :%count.*)
                                         (h/from dbname)
                                         sql/format))))
               [:users :groups :grouping])))

(defn initialize-user-environment [db admin-userinfo]
  (let [related-db-name [:users :groups :grouping]]
    (jdbc/with-transaction [tx (deref db)]
      (if (every? #(= % 0)
                  (map (fn [dbname]
                         (:count
                          (jdbc/execute-one! tx
                                             (-> (h/select :%count.*)
                                                 (h/from dbname)
                                                 sql/format))))
                       related-db-name))
        ;; If the DB for user processing is empty
        (do
          (jdbc/execute-one! tx
                             (-> (h/insert-into :users)
                                 (h/columns :uid :username :contact :passwdinfo)
                                 (h/values [[1
                                             (:username admin-userinfo)
                                             (:contact admin-userinfo)
                                             (hash-password (:password admin-userinfo))]])
                                 (sql/format {:pretty true})))
          (jdbc/execute-one! tx
                             (-> (h/insert-into :groups)
                                 (h/columns :gid :groupname)
                                 (h/values [[1 admin-group-name]])
                                 (sql/format {:pretty true})))
          (jdbc/execute-one! tx
                             (-> (h/insert-into :grouping)
                                 (h/columns :uid :gid)
                                 (h/values [[1 1]])
                                 (sql/format {:pretty true})))
          (jdbc/execute-one! tx
                             (-> (h/insert-into :groups)
                                 (h/columns :gid :groupname)
                                 (h/values [[2 default-group-name]])
                                 (sql/format {:pretty true})))
          (jdbc/execute-one! tx ["SELECT setval('users_uid_seq', (SELECT MAX(uid) FROM users));"])
          (jdbc/execute-one! tx ["SELECT setval('groups_gid_seq', (SELECT MAX(gid) FROM groups));"])
          true)
        false))))

(defn add-user-and-grouping [db userinfo]
  (let [related-db-name [:users :groups :grouping]]
    (jdbc/with-transaction [tx (deref db)]
      (let [result (jdbc/execute-one! tx
                                      (-> (h/insert-into :users)
                                          (h/columns :username :contact :passwdinfo)
                                          (h/values [[(:username userinfo)
                                                      (:contact userinfo)
                                                      (hash-password (:password userinfo))]])
                                          (sql/format {:pretty true}))
                                      {:return-keys true
                                       :builder-fn rs/as-unqualified-lower-maps})]
        (jdbc/execute-one! tx
                           (-> (h/insert-into :grouping)
                               (h/columns :uid :gid)
                               (h/values [[(:uid result) 2]])
                               (sql/format {:pretty true})))))))

(defn add-group [db groupinfo]
  (let [result (u/execute-one-sql
                db
                (-> (h/insert-into :groups)
                    (h/columns :groupname)
                    (h/values [[(:groupname groupinfo)]])
                    (sql/format {:pretty true})))]
    (= (:next.jdbc/update-count result) 1)))

(defn get-user [db uid]
  (let [result (unqualified
                (u/execute-one-sql
                 db
                 (-> (h/select :*)
                     (h/from :users)
                     (h/where [:= :uid uid])
                     (sql/format {:pretty true}))))]
    (if (empty? result)
      nil
      result)))

(defn get-user-by-username [db username]
  (let [result (unqualified
                (u/execute-one-sql
                 db
                 (-> (h/select :*)
                     (h/from :users)
                     (h/where [:= :username username])
                     (sql/format {:pretty true}))))]
    (if (empty? result)
      nil
      result)))

(defn get-user-groups-by-username [db username]
  (let [result 
        (u/execute-sql
         db
         (-> (h/select :grouping.gid
                       :groups.groupname)
             (h/from :users)
             (h/join :grouping [:= :users.uid :grouping.uid])
             (h/join :groups [:= :grouping.gid :groups.gid])
             (h/where [:= :username username])
             (sql/format {:pretty true})))]
    (if (empty? result)
      nil
      (map unqualified result))))

(defn get-group-by-groupname [db groupname]
  (let [result (unqualified
                (u/execute-one-sql
                 db
                 (-> (h/select :*)
                     (h/from :groups)
                     (h/where [:= :groupname groupname])
                     (sql/format {:pretty true}))))]
    (if (empty? result)
      nil
      result)))

(defn set-user-group [db uid groupname]
  (let [groupinfo (get-group-by-groupname db groupname)]
    (jdbc/with-transaction [tx (deref db)]
      (let [gid (if groupinfo
                  (:gid groupinfo)
                  (:gid (jdbc/execute-one! tx
                                           (-> (h/insert-into :groups)
                                               (h/columns :groupname)
                                               (h/values [[groupname]])
                                               (sql/format {:pretty true}))
                                           {:return-keys true
                                            :builder-fn rs/as-unqualified-lower-maps})))]
        (jdbc/execute-one! tx
                           (-> (h/insert-into :grouping)
                               (h/columns :uid :gid)
                               (h/values [[uid gid]])
                               (sql/format {:pretty true})))))))

(defn unset-user-group [db uid gid]
  (u/execute-one-sql db (-> (h/delete-from :grouping)
                            (h/where [:= :uid uid]
                                     [:= :gid gid])
                            (sql/format {:pretty true}))))

(defn belong? [db uid gid]
  (if (u/execute-one-sql db (-> (h/select :*)
                                (h/from :grouping)
                                (h/where [:= :uid uid]
                                         [:= :gid gid])
                                (sql/format {:pretty true})))
    true false))

(defn get-userlist [db]
  (map unqualified
       (u/execute-sql db
                      (-> (h/select :*)
                          (h/from :users)
                          (sql/format {:pretty true})))))

(defn get-grouplist [db]
  (map unqualified
       (u/execute-sql db
                      (-> (h/select :*)
                          (h/from :groups)
                          (sql/format {:pretty true})))))

(defn admingroup? [groups]
  (some #(= % 1) (map :gid groups)))


(defn- make-groups [tx grouplist]
  (let [groupset (set grouplist)
        db-groups (set (map #(:groups/groupname %)
                            (jdbc/execute! tx
                                           (-> (h/select :groupname)
                                               (h/from :groups)
                                               (h/where [:in :groupname grouplist])
                                               (sql/format {:pretty true})))))
        add-groups (cset/difference groupset db-groups)]
    (if (> (count add-groups) 0)
      (:next.jdbc/update-count
       (jdbc/execute-one! tx
                          (-> (h/insert-into :groups)
                              (h/columns :groupname)
                              (h/values (map vector (into [] add-groups)))
                              (sql/format {:pretty true}))))
      0)))

(defn- update-userinfo-groups [tx updates]
  (doseq [update updates]
    (when-let [groups (get update :groups)]
      (make-groups tx groups)
      (let [gids (set (map #(:groups/gid %)
                           (jdbc/execute! tx
                                          (-> (h/select :gid)
                                              (h/from :groups)
                                              (h/where [:in :groupname groups])
                                              (sql/format {:pretty true})))))
            user-gids (set (map #(:grouping/gid %)
                                (jdbc/execute! tx
                                               (-> (h/select :gid)
                                                   (h/from :grouping)
                                                   (h/where [:= :uid (:uid update)])
                                                   (sql/format {:pretty true})))))
            new-gids (cset/difference gids user-gids)
            del-gids (cset/difference user-gids gids)]
        {:new
         (if (> (count new-gids) 0)
           (:next.jdbc/update-count
            (jdbc/execute-one! tx
                               (-> (h/insert-into :grouping)
                                   (h/columns :uid :gid)
                                   (h/values (map (fn [gid] [(:uid update) gid])
                                                  (into [] new-gids)))
                                   (sql/format {:pretty true}))))
           0)
         :deleted
         (if (> (count del-gids) 0)
           (:next.jdbc/update-count
            (jdbc/execute-one! tx
                               (-> (h/delete-from :grouping)
                                   (h/where [:= :uid (:uid update)]
                                            [:in :gid del-gids])
                                   (sql/format {:pretty true}))))
           0)}))))

(defn- make-update-user-columns [db-info update]
  (let [update (replace-password->passwdhash update)]
    (when (= (:uid db-info) (:uid update))
      (let [update-keys (cset/intersection (set (keys db-info)) (set (keys update)))]
        (dissoc
         (into {}
               (filter (fn [[k v]]
                         (and (contains? update-keys k)
                              (not= (get db-info k) (get update k))))
                       update))
         :uid)))))

(defn- update-userinfo-infos [tx updates]
  (doseq [update updates]
    (if-let [db-info (jdbc/execute-one! tx
                                        (-> (h/select :*)
                                            (h/from :users)
                                            (h/where [:= :uid (:uid update)])
                                            (sql/format {:pretty true}))
                                        {:builder-fn rs/as-unqualified-lower-maps})]
      (let [new-info (make-update-user-columns db-info update)]
        (if-not (empty? new-info)
          (:next.jdbc/update-count
           (jdbc/execute-one! tx (-> (h/update :users)
                                     (h/set new-info)
                                     (h/where [:= :uid (:uid update)])
                                     (sql/format {:pretty true})))))
        0)
      0)))

(defn update-userinfo [db updates]
  (jdbc/with-transaction [tx (deref db)]
    {:groups (update-userinfo-groups tx updates)
     :infos (update-userinfo-infos tx updates)}))

(defn add-user [db userinfo]
  (let [exists (get-user-by-username db (:username userinfo))]
    (when-not exists
      (= (:next.jdbc/update-count
          (u/execute-one-sql
           db
           (-> (h/insert-into :users)
               (h/columns :username :contact :passwdinfo)
               (h/values [[(:username userinfo)
                           (:contact userinfo)
                           (hash-password (:password userinfo))]])
               (sql/format {:pretty true}))))
         1))))
