(ns secfarm.lecdata
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [secfarm.utils :as utils]))

(def course-file "defcourse.edn")
(def package-file "defpack.edn")

(defn- rename-old-course-keyname [course-info]
  (let [course-info (utils/rename-key-of-map course-info :contents :packages)]
    (assoc course-info :packages
           (map #(utils/rename-key-of-map % :directory :package)
                (get course-info :packages)))))

(defn- rename-old-package-keyname [package-info]
  (let [package-info (utils/rename-key-of-map package-info :files :contents)]
    (assoc package-info :contents
           (map #(utils/rename-key-of-map % :file :content)
                (get package-info :contents)))))

(defn- merge-package-config
  "Load the package settings and merges them with the course settings"
  [base-dir course-config]
  (assoc course-config
         :packages
         (filter #(not (nil? %))
                 (map (fn [content]
                        (let [pkg-dir (io/file base-dir (:package content))
                              pkg-conf (io/file pkg-dir package-file)]
                          (when (.isFile pkg-conf)
                            (assoc (-> (slurp pkg-conf)
                                       edn/read-string
                                       rename-old-package-keyname)
                                   :basedir
                                   pkg-dir))))
                      (:packages course-config)))))

(defn- load-course
  "Load one course data containing one or more package data from course-dir"
  [course-dir]
  (let [config-file (io/file course-dir course-file)]
    (if (.isFile config-file)
      (merge-package-config course-dir
                            (-> (slurp config-file)
                                edn/read-string
                                rename-old-course-keyname))
      {})))

(defn- load-lecdata
  "Load one lecture data containing one or more course data from lecdata-root-path if it exists"
  [lecdata-root-path]
  (filter #(not (empty? %))
          (map #(load-course %)
               (filter #(.isDirectory %) (.listFiles (io/file lecdata-root-path))))))

(defn load-lecdirs
  "Load several lecture data from lecdata-root-list (a list of directory path)"
  [lecdata-root-list]
  (flatten (map load-lecdata lecdata-root-list)))

(defrecord Lecdata [lecinfo config]
  component/Lifecycle
  (start [this]
    (let [config-data (load-lecdirs (-> (:option config)
                                        :server
                                        :lecdata))]
      (log/info (doall config-data))
      (assoc this :lecinfo config-data)))
  (stop [this]
    this))
