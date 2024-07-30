(ns secfarm.handlers.manage.api
  (:require [buddy.hashers :as hashers]
            [clojure.set :as cset]
            [java-time]
            [ring.util.response :as response]
            [selmer.parser :as selmer]
            [secfarm.data.user :as user]
            [secfarm.data.course :as course]
            [secfarm.data.lecdata :as lecdata]
            [secfarm.data.lecinfo :as lecinfo]
            [secfarm.handlers.utils :as u]))

(defn get-userlist [db req]
  (u/response-json 200
                   (->> (user/get-userlist db)
                        (map #(assoc
                               %
                               :group
                               (user/get-user-groups-by-username
                                db
                                (:username %)))))))

(defn get-grouplist [db req]
  (u/response-json 200 (user/get-grouplist db)))

(defn get-raw-courselist [lecinfo req]
  (u/response-json 200 lecinfo))

(defn get-privilege-info [db req]
  (let [string-gid (get (:params req) "gid")
        gid (try
              (Integer/parseInt string-gid)
              (catch Exception e nil))]
    (if gid
      (u/response-json 200 (course/get-privilege-by-gid db gid))
      (u/response-json 400 []))))

(defn- privilege-update-data? [groups lecinfo data]
  (let [gids (set (map #(get % :gid) groups))
        lec-ids (set (map #(get % :id) lecinfo))
        data-lec-ids (set (map #(get % :cid) data))
        data-grp-ids (set (map #(get % :gid) data))]
    (and (cset/subset? data-lec-ids lec-ids)
         (cset/subset? data-grp-ids gids))))

(defn set-privilege [db lecinfo {:keys [body-params] :as req}]
  (let [grouped-params (group-by (fn [a]
                                   (str (:gid a) "-" (:cid a)))
                                 body-params)
        merged-params (map #(reduce merge (get grouped-params %))
                           (keys grouped-params))
        grouplist (user/get-grouplist db)]
    (if (privilege-update-data? grouplist lecinfo merged-params)
      (do
        (course/set-privilege db merged-params)
        (u/response-json 200 {}))
      (u/response-json 400 {}))))

(defn get-user-progress [db lecinfo req]
  (let [uid (try (Integer/parseInt (get (:params req) "uid"))
                 (catch Exception e nil))]
    (if uid
      (let [content-list (lecinfo/make-contents-list lecinfo)
            progress-data (lecdata/get-all-progress-list db uid)
            record-data (lecdata/get-latest-record-list db uid)]
        (u/response-json
         200
         (map (fn [content]
                (cond
                  (= :doc (:type content))
                  (assoc content
                         :progress
                         (get (first (filter
                                      #(and (= (:cid content)
                                               (:course_id %))
                                            (= (:pid content)
                                               (:package_id %))
                                            (= (:content content)
                                               (:content_path %)))
                                      progress-data))
                              :progress
                              0))
                  (= :test (:type content))
                  (let [record (first (filter
                                       #(and (= (:cid content)
                                                (:course_id %))
                                             (= (:pid content)
                                                (:package_id %))
                                             (= (:content content)
                                                (:content_path %)))
                                       record-data))]
                    (assoc content
                           :score
                           (get record :score 0)
                           :scoremax
                           (get record :scoremax nil)))))
              content-list)))
      (u/response-json 400 []))))

(defn reset-test-record [db req]
  (let [uid (try (Integer/parseInt (get (:params req) "uid"))
                 (catch Exception e nil))
        cid (get (:params req) "cid")
        pid (get (:params req) "pid")
        cpath (get (:params req) "content")]
    (if (and uid cid pid cpath)
      (u/response-json 200 {:result (lecdata/remove-record db uid cid pid cpath)})
      (u/response-json 400 []))))

(defn reset-content-progress [db req]
  (let [uid (try (Integer/parseInt (get (:params req) "uid"))
                 (catch Exception e nil))
        cid (get (:params req) "cid")
        pid (get (:params req) "pid")
        cpath (get (:params req) "content")]
    (if (and uid cid pid cpath)
      (u/response-json 200 {:result (lecdata/remove-progress db uid cid pid cpath)})
      (u/response-json 400 []))))

(defn modify-userinfo [db {:keys [body-params] :as req}]
  (u/response-json 200 {:update (user/update-userinfo db body-params)}))
