(ns secfarm.handlers.lecture.render
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [selmer.parser :as selmer]
            [ring.util.response :as response]
            [ring.util.mime-type :as mime]
            [markdown.core :refer [md-to-html-string]]
            [secfarm.utils :refer [extract-file-extension make-counter]]
            [secfarm.handlers.utils :as u]
            [secfarm.data.lecinfo :as lec]))

(defn distinguish-file-type [file]
  (let [ext (extract-file-extension file)]
    (cond
      (or (= ext "md") (= ext "markdown"))
      :markdown
      (or (= ext "html" (= ext "htm")))
      :html)))

(defmulti render-doc distinguish-file-type)

(def ^:private md-html-template
  "<!DOCTYPE html>
<html>
<head>
<script src=\"/public/scripts/libs/highlight.min.js\"></script>
<link rel=\"stylesheet\" href=\"/public/css/highlight-style.min.css\"></link>
<script>hljs.highlightAll();</script>
</head>
<body>
<main id=\"markdown\">
%s
</main>
</body>
</html>")

(defmethod render-doc :markdown [file]
  (try
    (let [md-text (slurp file)
          html-str (md-to-html-string md-text :heading-anchors true)]
      (println (format md-html-template html-str))
      (u/response-html (format md-html-template html-str)))
    (catch Exception e
      (u/response-plain 500 "Failed to render file"))))

(defmethod render-doc :html [file]
  (try
    (let [htmlfile (slurp file)]
      (u/response-html htmlfile))
    (catch Exception e
      (u/response-plain 500 "Failed to render file"))))

(defmethod render-doc :default [file]
  (-> file
      (response/response)
      (response/content-type (mime/ext-mime-type (.getName file)))))

(defn render-package-resource [file]
  (if (.isFile file)
    (render-doc file)
    (u/response-plain 404 "File not found")))

(defmulti render #(keyword (get % :type)))

(defmethod render :doc [fileinfo]
  (u/response-html
   (selmer/render-file "templates/pages/lecture/lecture-viewer.html"
                       {:title (:content fileinfo)
                        :nickname (:nickname fileinfo)
                        :main (format "/lecture/file/%s/%s/%s"
                                      (:course-id fileinfo)
                                      (:package-id fileinfo)
                                      (:content fileinfo))})))

(defn- sort-tests [order tests]
  (let [counter (make-counter 0)
        serial-tests (map #(assoc % :serial (counter)) tests)]
    (case order
      :random (shuffle serial-tests)
      :accending (sort-by :serial serial-tests)
      :descending (sort-by :serial > serial-tests)
      serial-tests)))

(defn- make-test-page [definition]
  (u/response-html
   (selmer/render-file
    "templates/pages/lecture/test-viewer.html"
    {:test-title (if-let [title (-> (:config definition) :title)]
                   title
                   "Test")
     :tests (sort-tests
             (if-let [order (-> (:config definition) :order)]
               order
               :accending)
             (-> (get definition :tests [])))})))

(defn read-test-definition [fileinfo]
  (let [definition-file (io/file (:basedir fileinfo) (:content fileinfo))]
    (when (and (.isFile definition-file)
               (= "edn" (extract-file-extension definition-file)))
      (try
        (edn/read-string (slurp definition-file))
        (catch Exception e nil))))) ;; TODO: logging

(defmethod render :test [fileinfo]
  (if-let [testdef (read-test-definition fileinfo)]
    (make-test-page testdef)
    (u/response-plain 500 "Failed to render file")))

(defmethod render :default [fileinfo]
  (u/response-plain 400 "Invalid Type"))

(defn render-answers [definition answerdata]
  (u/response-html
   (selmer/render-file "templates/pages/lecture/answer-viewer.html"
                       {:title (str (if-let [title (-> (:config definition) :title)]
                                      title
                                      "Test")
                                    " Result")
                        :maxscore (count (:questions (:result answerdata)))
                        :score (:score (:result answerdata))
                        :answers (map (fn [test answer] (merge test (into {} answer)))
                                      (get definition :tests [])
                                      (:results (:result answerdata)))})))
