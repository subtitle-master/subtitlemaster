(ns smgui.ttual)

(defn in? [seq elm]
  (some #(= elm %) seq))

(defn not-in? [seq elm]
  (not (in? seq elm)))
