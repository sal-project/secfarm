(ns secfarm.handlers.lecture.page
  (:require [buddy.hashers :as hashers]
            [clojure.java.io :refer [file]]
            [java-time]
            [ring.util.response :as response]
            [selmer.parser :as selmer]
            [secfarm.lecdata :as lecdata-reader]
            [secfarm.handlers.utils :as u]
            [secfarm.handlers.lecture.render :as render]
            [secfarm.data.user :as user]
            [secfarm.data.course :as course]
            [secfarm.data.lecinfo :as lec]
            [secfarm.data.lecdata :as lecdata]))

(defmacro ^:private with-ids [[ids req] & exps]
  (let [id-names [:course :package :file]
        getters (map (fn [id] `(~id (:path-params ~req))) id-names)]
    `(let [~@(interleave ids getters)]
       ~@exps)))

(defmacro ^:private if-ids [[ids req] true-exp false-exp]
  `(with-ids [~ids ~req]
     (if (and ~@ids)
       ~true-exp
       ~false-exp)))

(defn- inject-group-priv [lecinfo db groups]
  (->> (map :gid groups)
       (course/get-privilege-by-gid db)
       (lec/add-privinfo lecinfo)))

(defn- answerhash->list [answers]
  (let [qnum-and-answers (map (fn [key] {:qnum (Integer. key)
                                         :answer (get answers key)})
                              (keys answers))]
    (map :answer (sort-by :qnum qnum-and-answers))))

(defn root [req]
  (u/response-html
   (selmer/render-file
    "templates/pages/lecture/list-viewer.html"
    {:list-target "Courses"})))

;; OPTIMIZE: /lecture以下のアクセス権チェックがページ/APIアクセス毎に複数回発生する仕様はどうにかしたほうがいいかも
(defn course-list [db lecinfo req]
  (with-ids [[course-id] req]
    (let [username (-> (:session req) :identity :username)
          groups (user/get-user-groups-by-username db username)
          privs (course/get-privilege-by-gid db (map :gid groups))
          lecinfo-with-priv (lec/add-privinfo lecinfo privs)]
      (if (or (lec/show? lecinfo-with-priv course-id)
              (user/admingroup? groups))
        (u/response-html
         (selmer/render-file
          "templates/pages/lecture/list-viewer.html"
          {:list-target "Packages"}))    
        (u/response-plain 404 "Package not found")))))

(defmulti render-package
  (fn [pkginfo] (:type (:control pkginfo))))

(defmethod render-package "simple" [pkginfo]
  (u/response-html
   (selmer/render-file
    "templates/pages/lecture/list-viewer.html"
    {:list-target "Contents"})))

(defmethod render-package "singlepage" [pkginfo]
  (let [singlefile (first (:contents pkginfo))]
    (response/redirect
     (format "/lecture/c/%s/%s/%s"
             (:parent pkginfo)
             (:id pkginfo)
             (:content singlefile)))))

(defmethod render-package :default [pkginfo]
  (u/response-html
   (selmer/render-file
    "templates/pages/lecture/list-viewer.html"
    {:list-target "Contents"})))

(defn course-package-list [db lecinfo req]
  (with-ids [[course-id package-id] req]
    (if-let [package-info (lec/get-info lecinfo course-id package-id)]
      (let [username (-> (:session req) :identity :username)
            groups (user/get-user-groups-by-username db username)
            privs (course/get-privilege-by-gid db (map :gid groups))
            lecinfo-with-priv (lec/add-privinfo lecinfo privs)]
        (if (or (lec/show? lecinfo-with-priv course-id)
                (user/admingroup? groups))
          (render-package (assoc package-info :parent course-id))
          (u/response-plain 404 "Package not found")))
      (u/response-plain 404 "Package not found"))))

(defn- available-resource? [target package-dir]
  (let [defpack-file (.getCanonicalPath (file package-dir lecdata-reader/package-file))
        canonical-target (.getCanonicalPath target)
        canonical-dir (.getCanonicalPath package-dir)]
    (and (not= canonical-target defpack-file)
         (.contains canonical-target (str canonical-dir (java.io.File/separator))))))

(defn contents-viewer [db lecinfo req]
  (if-ids [[course-id package-id content] req]
    (let [username (-> (:session req) :identity :username)
          groups (user/get-user-groups-by-username db username)
          lecinfo-with-priv (inject-group-priv lecinfo db groups)]
      (if (or (lec/attend? lecinfo-with-priv course-id)
              (user/admingroup? groups))
        (let [package-info (lec/get-info lecinfo course-id package-id)
              fileinfo (lec/get-fileinfo package-info content)]
          (if fileinfo
            (render/render (assoc fileinfo
                                  :basedir (:basedir package-info)
                                  :course-id course-id
                                  :package-id package-id
                                  :content content))
            (u/response-plain 404 "Contents not found")))
        (u/response-plain 403 "You cannnot attend this content.")))
    (u/response-plain 400 "Invalid Access")))

(defn package-contents [db lecinfo req]
  (if-ids [[course-id package-id content] req]
    (let [username (-> (:session req) :identity :username)
          groups (user/get-user-groups-by-username db username)
          lecinfo-with-priv (inject-group-priv lecinfo db groups)]
      (if (or (lec/attend? lecinfo-with-priv course-id)
              (user/admingroup? groups))
        (let [package-info (lec/get-info lecinfo course-id package-id)]
          (try
            (if (available-resource? (file (:basedir package-info) content)
                                     (file (:basedir package-info)))
              (render/render-package-resource (file (:basedir package-info) content))
              (u/response-plain 403 "You cannot access this area."))
            (catch Exception e
              (u/response-plain 500 "Internal server error"))))
        (u/response-plain 403 "You cannnot attend this content.")))
    (u/response-plain 400 "Invalid Access")))

(defn post-test-answers [db lecinfo {:keys [form-params] :as req}]
  (if-ids [[course-id package-id content] req]
    (let [username (-> (:session req) :identity :username)
          groups (user/get-user-groups-by-username db username)
          lecinfo-with-priv (inject-group-priv lecinfo db groups)]
      (if (or (lec/attend? lecinfo-with-priv course-id)
              (user/admingroup? groups))
        (let [package-info (lec/get-info lecinfo course-id package-id)
              fileinfo (lec/get-fileinfo package-info content)]
          (if fileinfo
            (try
              (if (available-resource? (file (:basedir package-info) content)
                                       (file (:basedir package-info)))
                (let [testdef (render/read-test-definition (assoc fileinfo
                                                                  :basedir
                                                                  (:basedir package-info)))
                      result (lec/grade testdef (answerhash->list form-params))
                      uid (:uid (user/get-user-by-username db username))]
                  (lecdata/record db uid {:course-id course-id
                                          :package-id package-id
                                          :content-path content
                                          :result result})
                  (render/render-answers testdef
                                         {:course-id course-id
                                          :package-id package-id
                                          :content-path content
                                          :result result}))
                (u/response-plain 403 "You cannot access this area."))
              (catch Exception e
                (u/response-plain 500 (str "Internal server error" e))))
            (u/response-plain 405 "This contents does not accept POST request.")))
        (u/response-plain 403 "You cannnot attend this content.")))
    (u/response-plain 400 "Invalid Access")))
