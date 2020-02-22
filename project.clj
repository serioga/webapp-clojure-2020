(defproject name.trofimov/webapp-clojure-2020 "1.0.0-SNAPSHOT"
  :description "Multi-page web application prototype with Clojure(Script)"
  :dependencies [; clojure
                 [org.clojure/clojure "1.10.1"]
                 ; clojure script
                 [cljsjs/react "16.12.0-2"]
                 [cljsjs/react-dom "16.12.0-2"]
                 [org.clojure/clojurescript "1.10.597" :scope "provided"]
                 [rum "0.11.4"]
                 ; shadow cljs
                 [com.google.javascript/closure-compiler-externs "v20191027" :scope "provided" :upgrade false]
                 [com.google.javascript/closure-compiler-unshaded "v20191027" :scope "provided" :upgrade false]
                 [org.clojure/google-closure-library "0.0-20191016-6ae1f72f" :scope "provided"]
                 [thheller/shadow-cljs "2.8.83" :scope "provided"]
                 ; system
                 [integrant "0.8.0"]
                 [mount "0.1.16"]
                 [tolitius/mount-up "0.1.2"]
                 ; web server
                 [metosin/reitit-core "0.4.2"]
                 [org.immutant/web "2.1.10"]
                 [ring/ring-core "1.8.0"]
                 [ring/ring-defaults "0.3.2"]
                 ; sql database
                 [com.h2database/h2 "1.4.200"]
                 [com.layerware/hugsql "0.5.1"]
                 [com.layerware/hugsql-adapter-next-jdbc "0.5.1"]
                 [com.mattbertolini/liquibase-slf4j "2.0.0"]
                 [com.zaxxer/HikariCP "3.4.2"]
                 [org.liquibase/liquibase-core "3.8.6"]
                 [seancorfield/next.jdbc "1.0.13"]
                 [p6spy/p6spy "3.8.7"]
                 ; logging (clojure)
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [ch.qos.logback/logback-core "1.2.3"]
                 [org.clojure/tools.logging "0.6.0"]
                 [org.codehaus.janino/janino "3.1.0"]
                 [org.slf4j/jul-to-slf4j "1.7.30"]
                 ; libs
                 [clojurewerkz/propertied "1.3.0"]
                 [com.cognitect/transit-clj "1.0.324"]
                 [com.taoensso/truss "1.5.0"]
                 [commons-codec/commons-codec "1.14"]
                 [org.apache.commons/commons-lang3 "3.9"]
                 [sablono "0.8.6"]
                 ; daemon
                 [commons-daemon/commons-daemon "1.2.2"]]

  :main ^:skip-aot app.main
  :test-paths ["test" "src"]
  :target-path "target/%s"
  :plugins [[lein-shell "0.5.0"]]

  :clean-targets ^{:protect false} ["target"
                                    "resources/public/app"]

  :repl-options {:init-ns dev.main}

  :shell {:commands
          {"node_modules/.bin/postcss"
           {:windows "node_modules/.bin/postcss.cmd"}}}
  
  :aliases {"shadow-cljs" ["run" "-m" "shadow.cljs.devtools.cli"]

            "css-example" ["shell"
                           "node_modules/.bin/postcss"
                           "tailwind/app/web_example/main.css"
                           "-o" "resources/public/app/example/main.css"]}

  :profiles {:dev {:jvm-opts ["-Dconfig.file=dev-resources/dev/config/default.props"]
                   :main ^:skip-aot dev.main
                   :dependencies [[nrepl "0.6.0"]
                                  [ns-tracker "0.4.0"]
                                  [ring-refresh "0.1.2"]
                                  [ring/ring-devel "1.8.0"]
                                  [zcaudate/hara.io.watch "2.8.7"]]
                   :source-paths ["dev" "tailwind"]}

             :test-release [:uberjar
                            {:jvm-opts ["-Dconfig.file=dev-resources/dev/config/default.props"]}]

             :uberjar {:aot :all
                       :prep-tasks ["compile"
                                    ["shadow-cljs" "release" "example"]
                                    "css-example"]}}

  :uberjar-name "website.jar")
