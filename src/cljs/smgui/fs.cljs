(ns smgui.fs)

(def fs (js/require "fs"))

(defn copy [source target]
  (let [input (.createReadStream fs source)
        output (.createWriteStream fs target)]
    (.pipe input output)))
