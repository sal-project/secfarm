(ns secfarm.data.lecdata
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [honey.sql.helpers :as h]
            [honey.sql :as sql]
            [muuntaja.core :as m]
            [secfarm.utils :refer [unqualified]]
            [secfarm.data.utils :as u]))

(defn latest-revision [db uid course-id package-id content-path]
  (let [revs (map #(:revisionnum (unqualified %))
                  (u/execute-sql
                   db
                   (-> (h/select :revisionnum)
                       (h/from :testresultlog)
                       (h/where [:= :uid uid]
                                [:= :course_id course-id]
                                [:= :package_id package-id]
                                [:= :content_path content-path])
                       (sql/format {:pretty true}))))]
    (if-not (empty? revs)
      (apply max revs)
      -1)))

(defn record [db uid result]
  (let [latest-rev (latest-revision db
                                    uid
                                    (:course-id result)
                                    (:package-id result)
                                    (:content-path result))]
    (u/execute-one-sql
     db
     (-> (h/insert-into :testresultlog)
         (h/columns :uid :course_id :package_id :content_path :revisionnum :scoremax :score :answers :incomplete)
         (h/values [[uid
                     (:course-id result)
                     (:package-id result)
                     (:content-path result)
                     (+ 1 latest-rev)
                     (count (:questions (:result result)))
                     (:score (:result result))
                     (pr-str (map #(into {} %) (:results (:result result))))
                     (not (:complete (:result result)))]])
         (sql/format {:pretty true})))))

(defn get-records [db uid course-id package-id content-path]
  (map unqualified
       (u/execute-sql
        db
        (-> (h/select :*)
            (h/from :testresultlog)
            (h/where [:= :uid uid]
                     [:= :course_id course-id]
                     [:= :package_id package-id]
                     [:= :content_path content-path])
            (sql/format {:pretty true})))))

(defn get-latest-record [db uid course-id package-id content-path]
  (let [records (get-records db uid course-id package-id content-path)]
    (when-not (empty? records)
      (apply max-key :revisionnum records))))

(defn get-course-progress [db uid course-id package-id content-path]
  (unqualified
   (u/execute-one-sql
    db
    (-> (h/select :*)
        (h/from :courseprog)
        (h/where [:= :uid uid]
                 [:= :course_id course-id]
                 [:= :package_id package-id]
                 [:= :content_path content-path])
        (sql/format {:pretty true})))))

(defn get-progress-list [db uid course-id]
  (map unqualified
   (u/execute-sql
    db
    (-> (h/select :*)
        (h/from :courseprog)
        (h/where [:= :uid uid]
                 [:= :course_id course-id])
        (sql/format {:pretty true})))))

(defn get-all-progress-list [db uid]
  (map unqualified
       (u/execute-sql
        db
        (-> (h/select :*)
            (h/from :courseprog)
            (h/where [:= :uid uid])
            (sql/format {:pretty true})))))

(defn get-latest-record-list [db uid]
  (let [records (map unqualified
                     (u/execute-sql
                      db
                      (-> (h/select :*)
                          (h/from :testresultlog)
                          (h/where [:= :uid uid])
                          (sql/format {:pretty true}))))
        grouped-records (map #(second %)
                             (group-by #(str (:course_id %) "-" (:package_id %) "-" (:content_path %))
                                       records))]
    (map #(apply max-key :revisionnum %) grouped-records)))

(defn set-course-progress [db uid course-id package-id content-path progress]
  (let [exists-data (get-course-progress db uid course-id package-id content-path)]
    (if-not (empty? exists-data)
      (u/execute-one-sql
       db
       (-> (h/update :courseprog)
           (h/set {:progress progress})
           (h/where [:= :uid uid]
                    [:= :course_id course-id]
                    [:= :package_id package-id]
                    [:= :content_path content-path])
           (sql/format {:pretty true})))
      (u/execute-one-sql
       db
       (-> (h/insert-into :courseprog)
           (h/columns :uid :course_id :package_id :content_path :progress)
           (h/values [[uid
                       course-id
                       package-id
                       content-path
                       progress]])
           (sql/format {:pretty true}))))))

(defn remove-record [db uid course-id package-id content-path]
  (u/execute-one-sql db
                     (-> (h/delete-from :testresultlog)
                         (h/where [:= :uid uid]
                                  [:= :course_id course-id]
                                  [:= :package_id package-id]
                                  [:= :content_path content-path])
                         (sql/format {:pretty true}))))

(defn remove-progress [db uid course-id package-id content-path]
  (u/execute-one-sql db
                     (-> (h/delete-from :courseprog)
                         (h/where [:= :uid uid]
                                  [:= :course_id course-id]
                                  [:= :package_id package-id]
                                  [:= :content_path content-path])
                         (sql/format {:pretty true}))))

(defn reset-all-progress [db]
  (jdbc/with-transaction [tx (deref db)]
    (jdbc/execute-one! tx
                       (-> (h/delete-from :testresultlog)
                           (sql/format {:pretty true})))
    (jdbc/execute-one! tx
                       (-> (h/delete-from :courseprog)
                           (sql/format {:pretty true})))))
