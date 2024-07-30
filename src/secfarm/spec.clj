(ns secfarm.spec
  (:require [clojure.spec.alpha :as s]))

(s/def ::username (s/and string?
                         #(> (count %) 0)))

(s/def ::password (s/and string?
                         #(> (count %) 0)))

(s/def ::contact (s/and string?
                        #(> (count %) 0)))

(s/def ::groups (s/coll-of (s/and string?
                                  #(> (count %) 0))))

(s/def ::locked boolean?)

(s/def ::currentpassword (s/and string?
                                #(> (count %) 0)))

(s/def ::newpassword (s/and string?
                            #(> (count %) 0)))

(s/def ::uid pos-int?)

(s/def ::gid pos-int?)

(s/def ::cid (s/and string?
                    #(re-matches #"^[a-zA-Z0-9][a-zA-Z0-9-]*" %)))

(s/def ::execute boolean?)

(s/def ::attend boolean?)

(s/def ::show boolean?)

(s/def ::login-request (s/keys :req-un [::username ::password]))

(s/def ::registration-request (s/keys :req-un [::username ::contact ::password]))

(s/def ::changepassword-request (s/keys :req-un [::currentpassword ::newpassword]))

(s/def ::userinfo-update (s/keys :req-un [::uid]
                                 :opt-un [::username
                                          ::password
                                          ::groups
                                          ::locked]))

(s/def ::userinfo-updates (s/coll-of ::userinfo-update))

(s/def ::set-privilege (s/keys :req-un [::gid ::cid]
                               :opt-un [::attend
                                        ::show
                                        ::execute]))

(s/def ::set-privileges (s/coll-of ::set-privilege))
