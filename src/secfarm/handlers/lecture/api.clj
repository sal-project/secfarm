(ns secfarm.handlers.lecture.api
  (:require [buddy.hashers :as hashers]
            [clojure.string :as cstr]
            [clojure.java.io :as io]
            [java-time]
            [ring.util.response :as response]
            [selmer.parser :as selmer]
            [secfarm.handlers.utils :as u]
            [secfarm.data.user :as user]
            [secfarm.data.course :as course]
            [secfarm.data.lecinfo :as lec]
            [secfarm.data.lecdata :as lecdata]))

(defn- extract-course-info [infos]
  {:title (:title infos)
   :id (:id infos)
   :description (:description infos)
   :license (:license infos)
   :priv [(or (get infos :show) false)
          (or (get infos :attend) false)
          (or (get infos :execute) false)]})

(defn get-courseinfo [db lecinfo req]
  (let [username (-> (:session req) :identity :username)
        groups (user/get-user-groups-by-username db username)
        privs (course/get-privilege-by-gid db (map :gid groups))
        lecinfo-with-priv (lec/add-privinfo lecinfo privs)
        readable-ids (lec/filter-readable lecinfo-with-priv)]
    (u/response-json
     200
     (map extract-course-info
          (if (some #(= % 1) (map :gid groups))
            lecinfo-with-priv
            readable-ids)))))

(defn- extract-package-info [infos]
  {:course_id (:id infos)
   :license (:license infos)
   :priv [(or (get infos :show) false)
          (or (get infos :attend) false)
          (or (get infos :execute) false)]
   :packages (:packages infos)})

(defn get-pkginfo [db lecinfo req]
  (let [id (get (:params req) "pid")
        username (-> (:session req) :identity :username)
        groups (user/get-user-groups-by-username db username)
        privs (course/get-privilege-by-gid db (map :gid groups))
        lecinfo-with-priv (lec/add-privinfo lecinfo privs)
        readable-ids (lec/filter-readable lecinfo-with-priv)
        result (if (some #(= % 1) (map :gid groups))
                 (first (filter #(= id (:id %)) lecinfo-with-priv))
                 (first (filter #(= id (:id %)) readable-ids)))]
    (if result
      (u/response-json
       200
       (extract-package-info result))
      (u/response-json
       404
       {}))))

(defn- extract-contents-info [db uid infos package-id]
  (when-let [package (first (filter #(= (:id %) package-id) (:packages infos)))]
    {:course_id (:id infos)
     :package_id (:id package)
     :license (:license infos)
     :control (:control package)
     :priv [(or (get infos :show) false)
            (or (get infos :attend) false)
            (or (get infos :execute) false)]
     :contents (map (fn [fileinfo]
                      (assoc fileinfo
                             :content
                             (:content fileinfo) ;; (.getName (io/file (:content fileinfo)))
                             :extra
                             (cond
                               (= :test (:type fileinfo))
                               (when-let [latest-record (lecdata/get-latest-record db
                                                                                   uid
                                                                                   (:id infos)
                                                                                   (:id package)
                                                                                   (.getName (io/file (:content fileinfo))))]
                                 (select-keys
                                  latest-record
                                  [:scoremax :score :revisionnum]))
                               (= :doc (:type fileinfo))
                               (when-let [latest-record (lecdata/get-course-progress db
                                                                                     uid
                                                                                     (:id infos)
                                                                                     (:id package)
                                                                                     (.getName (io/file (:content fileinfo))))]
                                 {:progress (get latest-record :progress 0)})
                               :default
                               {})))
                    (:contents package))}))

(defn get-contents [db lecinfo req]
  (let [cid (get (:params req) "cid")
        pid (get (:params req) "pid")
        username (-> (:session req) :identity :username)
        uid (:uid (user/get-user-by-username db username))
        groups (user/get-user-groups-by-username db username)
        privs (course/get-privilege-by-gid db (map :gid groups))
        lecinfo-with-priv (lec/add-privinfo lecinfo privs)
        readable-ids (lec/filter-readable lecinfo-with-priv)
        course (if (some #(= % 1) (map :gid groups))
                 (first (filter #(= cid (:id %)) lecinfo-with-priv))
                 (first (filter #(= cid (:id %)) readable-ids)))]
    (if (and cid pid)
      (if-let [result (extract-contents-info db uid course pid)]
        (u/response-json 200 result)
        (u/response-json 400 [:message "target not found"]))
      (u/response-json 400 [:message "cid or pid is not specified"]))))

(defn get-progress [db lecinfo req]
  (let [cid (get (:params req) "cid")
        username (-> (:session req) :identity :username)
        uid (:uid (user/get-user-by-username db username))
        groups (user/get-user-groups-by-username db username)
        privs (course/get-privilege-by-gid db (map :gid groups))
        lecinfo-with-priv (lec/add-privinfo lecinfo privs)]
    (if (or (lec/show? lecinfo-with-priv cid)
            (user/admingroup? groups))
      (u/response-json 200 (lecdata/get-progress-list db
                                                      uid
                                                      cid))
      (u/response-json 400 []))))

(defn mark-completed [db lecinfo req]
  (let [cid (get (:params req) "cid")
        pid (get (:params req) "pid")
        cpath (get (:params req) "content")
        username (-> (:session req) :identity :username)
        uid (:uid (user/get-user-by-username db username))
        groups (user/get-user-groups-by-username db username)
        privs (course/get-privilege-by-gid db (map :gid groups))
        lecinfo-with-priv (lec/add-privinfo lecinfo privs)]
    (if (or (lec/attend? lecinfo-with-priv cid)
            (user/admingroup? groups))
      (do (lecdata/set-course-progress db
                                       uid
                                       cid
                                       pid
                                       cpath
                                       100)
          (u/response-json 200 []))
      (u/response-json 400 []))))
