(ns smgui.components)

(defmacro pd [& body]
  `(fn [e#]
     (doto e#
       (.preventDefault e#)
       ~@body)))
