(ns secfarm.auth
  (:require [ring.util.response :as response]
            [secfarm.data.user :as user]))

(defn session-authenticated? [req]
  (let [session-value (:session req)]
    (boolean (:identity session-value))))

(defn admin-session-authenticated? [db req]
  (let [session-value (:session req)
        username (:username (:identity session-value))
        userinfo (user/get-user-by-username db username)]
    (user/belong? db (:uid userinfo) 1)))

(defn unauthorized-handler [req meta]
  (if (session-authenticated? req)
    (response/redirect "/")
    (response/redirect (format "/login"))))
