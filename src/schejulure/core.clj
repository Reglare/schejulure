(ns schejulure.core
  (:require [clj-time.core :refer [minute hour day month day-of-week]]
            [clj-time.local :refer [local-now]])
  (:import [java.util.concurrent Executors TimeUnit]))

(defn call-every-minute [f]
  (.scheduleAtFixedRate (Executors/newScheduledThreadPool 1) f 0 1 TimeUnit/MINUTES))

(defn cron-of [time]
  [(minute time)
   (hour time)
   (day time)
   (month time)
   (day-of-week time)])

(defn has? [coll item] (some #{item} coll))
(defn all? [coll] (every? identity coll))

(defn cron-match? [cron cron-range]
  (all? (map has? cron-range cron)))

(def cron-defaults {:minute (range 0 60)
                    :hour   (range 0 24)
                    :date   (range 1 32)
                    :month  (range 1 13)
                    :day    (range 0  7)})

(def day->number {:sun 0 0 0
                  :mon 1 1 1
                  :tue 2 2 2
                  :wed 3 3 3
                  :thu 4 4 4
                  :fri 5 5 5
                  :sat 6 6 6})

(def weekdays [:mon :tue :wed :thu :fri])

(defn keyword-day->number [x]
  (if (coll? x) (map day->number x)
      (list (day->number x))))

(defn cronmap->cronrange [cronmap]
  (map (fn [x] (if (coll? x) x (list x)))
       (-> (merge cron-defaults cronmap)
           (update-in [:day] keyword-day->number)
           ((juxt :minute :hour :date :month :day)))))

(defn fire-scheduled [scheduled-fns]
  (let [now (cron-of (local-now))]
    (doseq [[schedule f] scheduled-fns]
      (when (cron-match? now (cronmap->cronrange schedule))
        (try (f)
             (catch Exception e
               (println "Caught exception in scheduled action " f " at " now)
               (.printStackTrace e)))))))

(defn schedule
  "Takes pairs of cron-maps with the function to call when that cron-map
   matches the current time"
  [& args]
  (let [scheduled-fns (partition 2 args)]
    (call-every-minute #(fire-scheduled scheduled-fns))))
