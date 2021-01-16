(ns app.database.hugsql-adapter
  "Modified next.jdbc adapter for HugSQL."
  (:require [hugsql.adapter :as adapter]
            [hugsql.adapter.next-jdbc :as next-jdbc])
  (:import (hugsql.adapter HugsqlAdapter)))

(set! *warn-on-reflection* true)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- embed-fn-name
  "Adds function name in the body of SQL query as comment.
   For tracing SQL queries in logs."
  [sqlvec options]
  (assoc sqlvec 0 (-> "/* "
                      (.concat (name (options :fn-name)))
                      (.concat " */\n")
                      (.concat (sqlvec 0)))))

(comment
  (embed-fn-name ["SQL" 1 2] {:fn-name 'db-fn-name}) #_["/* db-fn-name */\nSQL" 1 2] #_"95 ns")

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn hugsql-adapter-next-jdbc
  "Modified next.jdbc adapter for HugSQL:
   - function names in the body of SQL queries."
  ([]
   (hugsql-adapter-next-jdbc {}))
  ([default-command-options]
   (let [a ^HugsqlAdapter (next-jdbc/hugsql-adapter-next-jdbc default-command-options)]
     (reify
       adapter/HugsqlAdapter
       (execute [this db sqlvec options] (.execute a db (embed-fn-name sqlvec options) options))
       (query [this db sqlvec options] (.query a db (embed-fn-name sqlvec options) options))
       (result-one [this result options] (.result_one a result options))
       (result-many [this result options] (.result_many a result options))
       (result-affected [this result options] (.result_affected a result options))
       (result-raw [this result options] (.result_raw a result options))
       (on-exception [this exception] (.on_exception a exception))))))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
