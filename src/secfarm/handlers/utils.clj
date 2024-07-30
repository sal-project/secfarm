(ns secfarm.handlers.utils
  (:require [ring.util.response :as res])  )

(defn response-html
  "Return utf-8 HTML response of view-ret"
  [view-ret]
  (-> (res/response view-ret)
      (res/content-type "text/html; charset=utf-8")))

(defn response-plain
  "Return plain-text response"
  [code body-text]
  {:status code
   :body body-text
   :headers {"content-type" "text/plain; charset=utf-8"}})

(defn response-json [status body]
  {:status status :body body :headers {"content-type" "application/json"}})
