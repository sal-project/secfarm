(ns secfarm.utils)

(defn unqualified [m]
  (into {} (map (fn [[k v]] [(keyword (name k)) v]) m)))

(defn extract-file-extension [file]
  (.substring (.getName file)
              (+ 1 (.lastIndexOf (.getName file) "."))))

(defn make-counter [initial]
  (let [count (atom (- initial 1))]
    (fn []
      (do (swap! count inc) @count))))

(defn rename-key-of-map [target oldkey newkey]
  (cond
    (and (get target oldkey) (get target newkey))
    (dissoc target oldkey)
    (get target oldkey)
    (dissoc (assoc target newkey (get target oldkey)) oldkey)
    :else
    target))
