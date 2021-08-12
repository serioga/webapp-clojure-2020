(ns lib.ring-middleware.route-tag-reitit
  (:require [lib.clojure.core :as e]
            [lib.clojure.perf :as p]
            [reitit.core :as reitit]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- fn'route->path
  [reitit-router, route-tag]
  (let [match (reitit/match-by-name reitit-router, route-tag)]
    (if (reitit/partial-match? match)
      ; Route with path parameters
      (let [required (:required match)]
        (fn param-route->path
          ([]
           (reitit/match-by-name! reitit-router, route-tag))
          ([params]
           (let [match (reitit/match-by-name! reitit-router, route-tag, (select-keys params required))]
             (if (== (count required) (count params))
               (reitit/match->path match)
               (reitit/match->path match (remove #(required (key %)) params)))))))
      ; Route without path parameters
      (fn simple-route->path
        ([]
         (reitit/match->path match))
        ([params]
         (reitit/match->path match params))))))

(defn- fn'path-for-route
  [reitit-router]
  (let [compiled (reduce (fn [m tag] (assoc m tag (fn'route->path reitit-router, tag)))
                         {} (reitit/route-names reitit-router))]
    (fn path-for-route
      ([route-tag]
       (when-some [route->path (compiled route-tag)]
         (route->path)))
      ([route-tag, params]
       (when-some [route->path (compiled route-tag)]
         (route->path params))))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn wrap-route-tag
  "Wrap handler with route-tag functionality."
  [handler, reitit-router]

  {:pre [(reitit/router? reitit-router)]}

  (fn route-tag
    [request]
    (let [match (reitit/match-by-path reitit-router, (request :uri))
          route-tag (-> match :data :name)
          path-params (-> match :path-params (e/asserted p/not-empty-coll?))]
      (handler (cond-> (p/fast-assoc request :route-tag/path-for-route (fn'path-for-route reitit-router))
                 route-tag,, (p/fast-assoc :route-tag route-tag)
                 path-params (update :params (fn merge-route-params [params]
                                               (p/fast-merge params path-params))))))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
