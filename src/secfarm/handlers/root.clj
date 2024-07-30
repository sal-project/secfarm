(ns secfarm.handlers.root
  (:require [buddy.hashers :as hashers]
            [clojure.tools.logging :as log]
            [java-time]
            [ring.util.response :as response]
            [selmer.parser :as selmer]
            [secfarm.data.user :as user]
            [secfarm.handlers.utils :as u]))

(defn root [req]
  (u/response-html
   (selmer/render-file "templates/pages/index.html" {})))

(defn login [req]
  (u/response-html
   (selmer/render-file "templates/pages/login.html" {})))

(defn authorization [db username password]
  (let [userinfo (user/get-user-by-username db username)]
    (if (and (not (:locked userinfo))
             (hashers/check password (:passwdinfo userinfo)))
      true
      false)))

(defn post-login [db {:keys [form-params] :as req}]
  (let [username (get form-params "username")
        password (get form-params "password")]
    (if (authorization db username password)
      (do
        (log/info (str "User: " username " Logging in"))
        (-> (response/redirect (get-in req [:query-param "next"] "/"))
            (assoc-in [:session :identity] {:username username})))
      (do
        (log/info (format "User: %s Login failed" username))
        (login req)))))

(defn logout [req]
  (-> (response/redirect "/")
      (assoc :session {})))

(defn whoami [req]
  (u/response-plain 200 (-> (:session req) :identity :username)))

(defn iadmin [db req]
  (let [username (-> (:session req) :identity :username)
        groups (user/get-user-groups-by-username db username)]
    (u/response-json 200 {:answer (if (user/admingroup? groups)
                                    true
                                    false)})))

(defn registration [db req]
  (let [need-init (user/empty-user? db)]
    (u/response-html
     (selmer/render-file "templates/pages/registration.html"
                         {:message ""
                          :need-init need-init}))))

(defn post-registration [db {:keys [form-params] :as req}]
  (let [username (get form-params "username")
        contact (get form-params "contact")
        password (get form-params "password")
        need-init (user/empty-user? db)]
    (if need-init
      (u/response-plain 405 "You need to initialize admin user first.")
      (if (user/get-user-by-username db username)
        (u/response-html
         (selmer/render-file "templates/pages/registration.html"
                             {:message "Conflict username"
                              :need-init need-init}))
        (do (user/add-user-and-grouping db
                                        {:username username
                                         :password password
                                         :contact contact})
            (response/redirect "/login"))))))
