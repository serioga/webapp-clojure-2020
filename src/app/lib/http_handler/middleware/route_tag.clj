(ns app.lib.http-handler.middleware.route-tag
  (:require
    [app.lib.util.perf :as perf]
    [reitit.core :as reitit]))

(set! *warn-on-reflection* true)


(defn wrap-route-tag
  [handler, reitit-router]

  {:pre [(reitit/router? reitit-router)]}

  (fn route-tag
    [{:keys [uri] :as request}]

    (let [{:keys [path-params], {:keys [name]} :data}
          (reitit/match-by-path reitit-router, uri)]

      (handler (cond-> request

                 (some? name)
                 (perf/fast-assoc :route-tag name)

                 (perf/not-empty-coll? path-params)
                 (update :params (fn merge-route-params
                                   [params]
                                   (perf/fast-merge params path-params))))))))

