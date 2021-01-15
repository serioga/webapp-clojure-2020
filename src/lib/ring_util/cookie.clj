(ns lib.ring-util.cookie)

(set! *warn-on-reflection* true)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn get-cookie-value
  "Reads cookie value from request."
  [request cookie-name]
  (get-in request [:cookies cookie-name :value]))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn set-cookie
  "Sets named cookie in ring response."
  [response cookie-name cookie]
  (let [c (assoc cookie :path "/"
                        #_#_#_#_:http-only true
                            :same-site :strict)]
    (update response :cookies assoc cookie-name c)))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn remove-cookie
  "Removes named cookie in ring response."
  [response cookie-name]
  (set-cookie response cookie-name {:value "removing_cookie_value..." :max-age -1}))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
