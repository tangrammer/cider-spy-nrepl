(ns cider-spy-nrepl.middleware.tracker-test
  (:require [cider-spy-nrepl.middleware.tracker :refer :all]
            [clojure.test :refer :all]))

(defmacro tracker-harness [& forms]
  `(let [~'session (atom {})]
     ~@forms))

;; Even though I'll remove tracking of whole messages, I'll test for now.
(deftest test-messages
  (tracker-harness
   (let [msg1 {:ns "foo-ns"}
         msg2 {:ns "foo2-ns"}]
     (track-msg! msg1 session)
     (track-msg! msg2 session)
     (is (= (list msg2 msg1)  (-> @session :tracking :messages))))))

(deftest test-track-namespace
  (tracker-harness
   (track-msg! {:ns "foo-ns"} session)
   (track-msg! {:ns "foo-ns2"} session)
   (is (= (list "foo-ns2" "foo-ns") (map :ns (-> @session :tracking :ns-trail))))))

(deftest test-track-command
  (tracker-harness
   (let [code "(println \"bob\")"]
     (track-msg! {:code code :ns "user"} session)
     (is (= 1 (get-in @session [:tracking :commands "clojure.core/println"])))
     (track-msg! {:code code :ns "user"} session)
     (is (= 2 (get-in @session [:tracking :commands "clojure.core/println"]))))))

;; Bit unsure of exactly when ns will be around.
(deftest test-track-ns-loaded-no-ns
  (tracker-harness
   (let [file "(ns foo.bar) (println \"hi\")"]
     (track-msg! {:op "load-file" :file file} session)
     (is (= 1 (get-in @session [:tracking :nses-loaded "foo.bar" :freq])))
     (is (= (list "foo.bar") (map :ns (-> @session :tracking :ns-trail))))
     (track-msg! {:op "load-file" :file file} session)
     (is (= 2 (get-in @session [:tracking :nses-loaded "foo.bar" :freq]))))))

;; test using clojure.core, I think this is a resource, not a file.
;; Should challenge the model.
;; Make this sucker work jp
;; TODO - do I need the frigging file on disk? Delete, bounce REPL and retest
;; TODO - test the ns-trail - this should have same shit going down
;; A pretty bad problem is that this tracker needs to be applied POST handling...
;; Probably a generic issue tbh
(deftest test-track-ns-loaded
  (require '[cider-spy-nrepl.middleware.sample-ns])
  ((resolve 'cider-spy-nrepl.middleware.sample-ns/foo))
  (tracker-harness
   (testing "Some is reloading the sample-ns, to make changes etc."
     (let [ns-str "cider-spy-nrepl.middleware.sample-ns"
           file (format "(ns %s) (defn bob []) (println \"hi\")" ns-str)]
       (track-msg! {:op "load-file" :file file} session)
       (is (= 1 (get-in @session [:tracking :nses-loaded ns-str :freq])))
       (println (get-in @session [:tracking :nses-loaded ns-str]))
       (let [file-location (get-in @session [:tracking :nses-loaded ns-str :file])]
         (is (and file-location
                  (re-find #"cider_spy_nrepl/middleware/sample_ns\.clj$" file-location)))
         ;; (is (and file-location
         ;;          (re-find #"/test/cider_spy_nrepl/middleware/sample_ns\.clj$" file-location))))
)))))

(deftest test-track-namespace-and-loaded
  (tracker-harness
   (track-msg! {:ns "foo-ns"} session)
   (track-msg! {:op "load-file" :file "(ns foo-ns) (println \"hi\")"} session)
   (is (= (list "foo-ns") (map :ns (-> @session :tracking :ns-trail))))))
