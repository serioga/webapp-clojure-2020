(ns app.lib.http-handler.middleware.route-tag
  (:require
    [app.lib.util.perf :as perf]
    [reitit.core :as reitit]))

(set! *warn-on-reflection* true)


(defn ^:private fn'route->path
  [reitit-router, route-tag]
  (let [match (reitit/match-by-name reitit-router, route-tag)]
    (if (reitit/partial-match? match)
      ; route with path parameters
      (let [required (:required match)]
        (fn param-route->path
          ([]
           (reitit/match-by-name! reitit-router, route-tag))
          ([params]
           (let [match (reitit/match-by-name! reitit-router, route-tag, (select-keys params required))]
             (if (== (count required) (count params))
               (reitit/match->path match)
               (reitit/match->path match (remove #(required (key %)) params)))))))
      ; route without path parameters
      (fn simple-route->path
        ([]
         (reitit/match->path match))
        ([params]
         (reitit/match->path match params))))))


(defn ^:private fn'path-for-route
  [reitit-router]
  (let [compiled (reduce (fn [m tag]
                           (assoc m tag (fn'route->path reitit-router, tag)))
                   {} (reitit/route-names reitit-router))]
    (fn path-for-route
      ([route-tag]
       (when-some [route->path (compiled route-tag)]
         (route->path)))
      ([route-tag, params]
       (when-some [route->path (compiled route-tag)]
         (route->path params))))))


(defn wrap-route-tag
  [handler, reitit-router]

  {:pre [(reitit/router? reitit-router)]}

  (fn route-tag
    [{:keys [uri] :as request}]

    (let [{:keys [path-params], {:keys [name]} :data}
          (reitit/match-by-path reitit-router, uri)]

      (handler (cond-> (perf/fast-assoc request
                         :route-tag/path-for-route (fn'path-for-route reitit-router))

                 (some? name)
                 (perf/fast-assoc :route-tag name)

                 (perf/not-empty-coll? path-params)
                 (update :params (fn merge-route-params
                                   [params]
                                   (perf/fast-merge params path-params))))))))

