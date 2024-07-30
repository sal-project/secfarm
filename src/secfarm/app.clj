(ns secfarm.app
  (:require [buddy.auth.backends.session :refer [session-backend]]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [buddy.auth.accessrules :refer [wrap-access-rules]]
            [com.stuartsierra.component :as component]
            [muuntaja.core :as m]
            [ring.middleware.session :as session]
            [reitit.ring :as reitit-ring]
            [reitit.ring.middleware.parameters :as params]
            [reitit.ring.coercion :as rrc]
            [reitit.coercion.spec :as rcs]
            [reitit.ring.middleware.muuntaja]
            [secfarm.auth :as auth]
            [secfarm.handlers.root :as root]
            [secfarm.handlers.my.page :as my]
            [secfarm.handlers.manage.page :as manage]
            [secfarm.handlers.manage.api :as manage-api]
            [secfarm.handlers.lecture.page :as lecture]
            [secfarm.handlers.lecture.api :as lecture-api]
            [secfarm.spec :as spec]))

(defn- define-route [db lecinfo]
  [["/public/*" (reitit-ring/create-resource-handler)]
   ["/" {:get root/root}]
   ["/registration" {:get #(root/registration db %)
                     :post {:parameters {:form ::spec/registration-request}
                            :handler #(root/post-registration db %)}}]
   ["/login" {:get root/login
              :post {:parameters {:form ::spec/login-request}
                     :handler #(root/post-login db %)}}]
   ["/logout" {:get root/logout}]
   ["/my" {:get my/root}]
   ["/my/history" {:get my/history}]
   ["/my/history-data" {:get #(my/history-data db lecinfo %)}]
   ["/my/change-password" {:get my/change-password
                           :post {:parameters {:form ::spec/changepassword-request}
                                  :handler #(my/post-change-password db %)}}]

   ["/whoami" {:get root/whoami}]
   ["/iadmin" {:get #(root/iadmin db %)}]
   ["/manage" {:get manage/root}]
   ["/manage/lecture" {:get manage/lecture}]
   ["/manage/progress" {:get manage/progress}]
   ["/manage/user" {:get manage/user}]
   ["/manage/api/get-userlist" {:get #(manage-api/get-userlist db %)}]
   ["/manage/api/get-grouplist" {:get #(manage-api/get-grouplist db %)}]
   ["/manage/api/get-raw-courselist" {:get #(manage-api/get-raw-courselist lecinfo %)}]
   ["/manage/api/get-privilege-info" {:get #(manage-api/get-privilege-info db %)}]
   ["/manage/api/set-privilege" {:post {:parameters {:body ::spec/set-privileges}
                                        :handler #(manage-api/set-privilege db lecinfo %)}}]
   ["/manage/api/get-user-progress" {:get #(manage-api/get-user-progress db lecinfo %)}]
   ["/manage/api/reset-test-record" {:get #(manage-api/reset-test-record db %)}]
   ["/manage/api/reset-content-progress" {:get #(manage-api/reset-content-progress db %)}]
   ["/manage/api/modify-userinfo" {:post {:parameters {:body ::spec/userinfo-updates}
                                          :handler #(manage-api/modify-userinfo db %)}}]
   ["/lecture" {:get lecture/root}] ;; アクセス権があるコース一覧
   ["/lecture/c/:course" {:get #(lecture/course-list db lecinfo %)}] ;; 指定コースのパッケージ一覧
   ["/lecture/c/:course/:package" {:get #(lecture/course-package-list db lecinfo %)}] ;; パッケージ内のindexコンテンツ一覧
   ["/lecture/c/:course/:package/*file" {:get #(lecture/contents-viewer db lecinfo %)  ;; パッケージ内のファイル
                                         :post #(lecture/post-test-answers db lecinfo %)}]
   ["/lecture/file/:course/:package/*file" {:get #(lecture/package-contents db lecinfo %)}]
   ["/lecture/api/get-courseinfo" {:get #(lecture-api/get-courseinfo db lecinfo %)}]
   ["/lecture/api/get-pkginfo" {:get #(lecture-api/get-pkginfo db lecinfo %)}]
   ["/lecture/api/get-contents" {:get #(lecture-api/get-contents db lecinfo %)}]
   ["/lecture/api/get-progress" {:get #(lecture-api/get-progress db lecinfo %)}]
   ["/lecture/api/mark-completed" {:get #(lecture-api/mark-completed db lecinfo %)}]])

(defn- make-routes [db]
  )

(defn- make-handler [config db lecinfo]
  (let [access-rule [{:pattern #"^/lecture.*" :handler auth/session-authenticated?}
                     {:pattern #"^/my.*" :handler auth/session-authenticated?}
                     {:pattern #"^/manage.*" :handler (fn [req] (auth/admin-session-authenticated? db req))}]
        backend (session-backend {:unauthorized-handler auth/unauthorized-handler})]
    (reitit-ring/ring-handler
     (reitit-ring/router (define-route db lecinfo)
                         {:data {:muuntaja m/instance
                                 :coercion rcs/coercion
                                 :middleware [params/parameters-middleware
                                              reitit.ring.middleware.muuntaja/format-middleware
                                              rrc/coerce-exceptions-middleware
                                              rrc/coerce-request-middleware
                                              rrc/coerce-response-middleware]}})
     (reitit-ring/routes (reitit-ring/redirect-trailing-slash-handler)
                         (reitit-ring/create-default-handler
                          {:not-found (constantly {:status 404 :body "Page not found"})
                           :method-not-allowd (constantly {:status 405 :body "Method not allowed"})
                           :not-acceptable (constantly {:status 406 :body "Not acceptable"})}))
     {:middleware [[session/wrap-session {:cookie-attrs {:http-only true
                                                         :same-site :strict
                                                         ;;:secure true ;; TODO: 設定ファイルによりON/OFF可能にする
                                                         :max-age 86400}}]
                   [wrap-authorization backend]
                   [wrap-authentication backend]
                   [wrap-access-rules {:rules access-rule
                                       :policy :allow}]]})))

(defrecord App [handler config db lecdata]
  component/Lifecycle
  (start [this]
    (assoc this :handler (make-handler (:option config)
                                       (:datasource db)
                                       (:lecinfo lecdata))))
  (stop [this]
    this))

