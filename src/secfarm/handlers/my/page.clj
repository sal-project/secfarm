(ns secfarm.handlers.my.page
  (:require [buddy.hashers :as hashers]
            [ring.util.response :as response]
            [selmer.parser :as selmer]
            [secfarm.handlers.root :as root]
            [secfarm.handlers.utils :as u]
            [secfarm.data.course :as course]
            [secfarm.data.user :as user]
            [secfarm.data.lecdata :as lecdata]
            [secfarm.data.lecinfo :as lecinfo]))

(defn root [req]
  (u/response-html
   (selmer/render-file "templates/pages/my/index.html" {})))

(defn history-data [db lecinfo req]
  (let [username (-> (:session req) :identity :username)
        uid (:uid (user/get-user-by-username db username))
        groups (user/get-user-groups-by-username db username)
        privs (course/get-privilege-by-gid db (map :gid groups))
        lecinfo-with-priv (lecinfo/add-privinfo lecinfo privs)
        readable-ids (lecinfo/filter-readable lecinfo-with-priv)
        content-list (lecinfo/make-contents-list readable-ids)
        progress-data (lecdata/get-all-progress-list db uid)
        record-data (lecdata/get-latest-record-list db uid)]
    (u/response-json 200
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
                          content-list))))

(defn history [req]
  (u/response-html
     (selmer/render-file
      "templates/pages/my/history.html"
      {})))

(defn change-password [req]
  (u/response-html
   (selmer/render-file "templates/pages/my/change-password.html" {})))

(defn post-change-password [db {:keys [form-params] :as req}]
  (let [username (-> (:session req) :identity :username)
        uid (:uid (user/get-user-by-username db username))
        current-password (get form-params "currentpassword")
        new-password (get form-params "newpassword")]
    (if (root/authorization db username current-password)
      (do
        (user/update-userinfo db [{:uid uid :password new-password}])
        (response/redirect (get-in req [:query-param "next"] "/")))
      (change-password req))))
