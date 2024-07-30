(ns secfarm.handlers.manage.page
  (:require [buddy.hashers :as hashers]
            [java-time]
            [ring.util.response :as response]
            [selmer.parser :as selmer]
            [secfarm.data.user :as user]
            [secfarm.handlers.utils :as u]))

(defn root [req]
  (u/response-html
   (selmer/render-file "templates/pages/manage/index.html" {})))

(defn lecture [req]
  (u/response-html
   (selmer/render-file "templates/pages/manage/lecture.html" {})))

(defn progress [req]
  (u/response-html
   (selmer/render-file "templates/pages/manage/progress.html" {})))

(defn user [req]
  (u/response-html
   (selmer/render-file "templates/pages/manage/user.html" {})))
