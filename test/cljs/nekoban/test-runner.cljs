(ns nekoban.test-runner
  (:require
   [cljs.test :refer-macros [run-tests]]
   [nekoban.core-test]))

(enable-console-print!)

(defn runner []
  (if (cljs.test/successful?
       (run-tests
        'nekoban.core-test))
    0
    1))
