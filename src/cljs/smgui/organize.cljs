(ns smgui.organize
  (:require-macros [swannodette.utils.macros :refer [dochan <?]]
                   [cljs.core.async.macros :refer [go]])
  (:require [om.dom :as dom]
            [om.core :as om]
            [smgui.fs :as fs]
            [smgui.gui :as gui]
            [cljs.core.async :refer [<! put! chan timeout]]
            [swannodette.utils.reactive :as r]))

(def nwpath (js/require "path"))

(defn basename [path]
  (.basename nwpath path))

(defn basename-without-extension [path]
  (.basename nwpath path (.extname nwpath path)))

(def video-extensions
  #{"3g2" "3gp" "3gp2" "3gpp" "60d" "ajp" "asf" "asx" "avchd" "avi"
    "bik" "bix" "box" "cam" "dat" "divx" "dmf" "dv" "dvr-ms" "evo" "flc"
    "fli" "flic" "flv" "flx" "gvi" "gvp" "h264" "m1v" "m2p" "m2ts" "m2v"
    "m4e" "m4v" "mjp" "mjpeg" "mjpg" "mkv" "moov" "mov" "movhd" "movie"
    "movx" "mp4" "mpe" "mpeg" "mpg" "mpv" "mpv2" "mxf" "nsv" "nut" "ogg"
    "ogm" "omf" "ps" "qt" "ram" "rm" "rmvb" "swf" "ts" "vfw" "vid"
    "video" "viv" "vivo" "vob" "vro" "wm" "wmv" "wmx" "wrap" "wvx" "wx"
    "x264" "xvid"})

(defn event->value [e] (-> e .-target .-value))

(defn has-video-extension? [path] (fs/match-extensions? path video-extensions))

(defn sample? [path] (boolean (re-find #"sample" path)))

(defn normalize [path]
  (-> (basename-without-extension path)
      (clojure.string/replace #"\." " ")))

(defn episode-info [path]
  (if-let [[_ name season episode] (re-find #"(?i)(.+)\sS(\d+)E(\d+)" (normalize path))]
    {:path path
     :name name
     :season season
     :episode episode}))

(defn str-pre-case [s str]
  (or
   (first (filter #(= (.toLowerCase %) (.toLowerCase str)) s))
   str))

(defn with-target [{:keys [name season path] :as info} target]
  (let [target-part (str name "/Season " season "/" (basename path))]
    (merge info {:target (str target "/" target-part)
                 :target-part target-part})))

(defn show-lookup
  ([path] (show-lookup path []))
  ([path names]
     (->> (fs/scandir path)
          (r/remove sample?)
          (r/filter has-video-extension?)
          (r/filter fs/is-file?)
          (r/mapfilter episode-info)
          (r/map #(update-in % [:name] (partial str-pre-case names))))))

(defn directory-picker [cursor owner]
  (reify
    om/IDidMount
    (did-mount [this]
      (-> this .-owner .-refs .-input .getDOMNode (.setAttribute "nwdirectory" "nwdirectory")))

    om/IRenderState
    (render-state [this {k :path}]
      (dom/div nil
        (dom/div nil (get cursor k))
        (dom/input #js {:type "file"
                        :ref "input"
                        :onChange #(om/update! cursor k (event->value %))})))))

(defn scan [{:keys [source target]} cursor]
  (let [{:keys [searching]} @cursor]
    (when-not searching
      (om/update! cursor [:searching] true)
      (om/update! cursor [:matched] [])
      (go
        (try
          (let [names (map basename (<? (fs/read-dir target)))]
            (<! (dochan [info (show-lookup source names)]
                  (om/transact! cursor [:matched] #(conj % (with-target info target))))))
          (catch js/Error e
            (.log js/console "Error" e)))
        (om/update! cursor [:searching] false)))))

(defn scan-result [{:keys [path name season target target-part]}]
  (dom/div nil
    (dom/div nil target-part)
    (dom/div nil target)
    (dom/button #js {:onClick #(do (.preventDefault %)
                                   (gui/show-file path))
                     :href "#"}
                "show")))

(defn render-organize [cursor]
  (let [settings (-> cursor :settings :organizer)
        {:keys [searching matched] :as organizer} (-> cursor :organizer)
        run-scan #(scan (-> @cursor :settings :organizer) organizer)]
    (dom/div #js {:className "flex auto-scroll"}
      (dom/div #js {:className "white-box"}
        "Video Organizer"
        (om/build directory-picker settings {:state {:path :source}})
        (om/build directory-picker settings {:state {:path :target}})
        (dom/button #js {:onClick run-scan
                         :disabled searching} "Scan"))
      (apply dom/div #js {:className "white-box"}
        (if searching
          "Searching...")
        (map scan-result matched)))))
