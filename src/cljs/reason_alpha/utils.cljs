(ns reason-alpha.utils)

(defn rand-between [min max]
  (+ (* (rand max) (- max min)) min))