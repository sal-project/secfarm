(ns secfarm.data.lecinfo)

(defn add-privinfo [lecinfo privinfo]
  (map (fn [course]
         (when-let [course-priv (filter #(= (:id course) (get % :course_id)) privinfo)]
           (merge course
                  {:show (some true? (map #(:show_p %) course-priv))
                   :attend (some true? (map #(:attend_p %) course-priv))
                   :execute (some true? (map #(:execute_p %) course-priv))})))
       lecinfo))

(defn filter-readable
  ([lecinfo-and-priv]
   (filter (fn [course]
             (or (:show course)
                 (:attend course)
                 (:execute course))) lecinfo-and-priv))
  ([lecinfo privinfo]
   (filter (fn [course]
             (when-let [course-priv (filter #(= (:id course) (get % :course_id)) privinfo)]
               (or (some true? (map #(:show_p %) course-priv))
                   (some true? (map #(:attend_p %) course-priv))
                   (some true? (map #(:execute_p %) course-priv)))))
           lecinfo)))

(defn show? [lecinfo-and-priv course]
  (if-let [courseinfo (first (filter #(= (:id %) course)
                                     lecinfo-and-priv))]
    (if (or (get courseinfo :show false)
            (get courseinfo :attend false)
            (get courseinfo :execute false))
      true
      false)
    false))

(defn attend? [lecinfo-and-priv course]
  (if-let [courseinfo (first (filter #(= (:id %) course)
                                     lecinfo-and-priv))]
    (if (or (get courseinfo :attend false)
            (get courseinfo :execute false))
      true
      false)
    false))

(defn get-info [lecinfo & ids]
  (let [course-id (first ids)
        package-id (second ids)]
    (when course-id
      (when-let [course (first (filter #(= (:id %) course-id) lecinfo))]
        (if-let [package (first (filter #(= (:id %) package-id) (:packages course)))]
          package
          course)))))

(defn get-fileinfo [packageinfo filename]
  (first (filter #(= (:content %) filename) (:contents packageinfo))))

(defrecord UserAnswer [answer result message])

(defmulti check-answer (fn [test answer] (get test :type)))

(defmethod check-answer :selection [test answer]
  (let [answer-index (.indexOf (:options test) answer)]
    (UserAnswer. answer
                 (if (= answer-index (- (:answer test) 1))
                   true
                   false)
                 "auto-assessment")))

(defmethod check-answer :input [test answer]
  (UserAnswer. answer
               (if (>= (.indexOf (:answer test) answer) 0)
                 true
                 false)
               "auto-assessment"))

(defmethod check-answer :multiselection [test answer]
  (let [answer-lst (if (sequential? answer) answer [answer])
        answer-num-lst (map #(.indexOf (:options test) %) answer-lst)
        answer-set (set answer-num-lst)]
    (UserAnswer. answer-lst
                 (= answer-set (set (map #(- % 1) (:answer test))))
                 "auto-assessment")))

(defmethod check-answer :description [test answer]
  (UserAnswer. answer
               nil
               "Not implemented yet"))

(defmethod check-answer :submission [test answer]
  (UserAnswer. answer
               nil
               "Not implemented yet"))

(defmethod check-answer :default [test answer]
  (UserAnswer. answer
               nil
               "Invalid test type"))

(defrecord GradeResult [questions score complete results])

(defn grade [testdef answers]
  (let [tests (:tests testdef)]
    (loop [question tests
           answers answers
           result []]
      (if-not (empty? question)
        (recur (rest question)
               (rest answers)
               (conj result (check-answer (first question) (first answers))))
        (GradeResult. tests
                      (count (filter #(= true (:result %)) result))
                      (= (count (filter #(nil? (:result %)) result)) 0)
                      result)))))

(defn make-contents-list [lecinfo]
  (for [{:keys [id packages]} lecinfo
        {pid :id contents :contents} packages
        {:keys [content type]} contents]
    {:cid id :pid pid :content content :type type}))
