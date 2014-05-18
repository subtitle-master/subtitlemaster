(ns smgui.uuid)

(def uuid-lib (.require js/window "node-uuid"))

(defn v4 [] (.v4 uuid-lib))
