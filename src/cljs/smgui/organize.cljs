(ns smgui.organize
  (:require-macros [wilkerdev.util.macros :refer [dochan <? go-catch]]
                   [cljs.core.async.macros :refer [go]])
  (:require [om.dom :as dom]
            [om.core :as om]
            [sm.util :refer [episode-info]]
            [smgui.core :refer [flux-channel]]
            [smgui.gui :as gui]
            [cljs.core.async :refer [<! put! chan timeout]]
            [wilkerdev.util.reactive :as r]
            [wilkerdev.util.nodejs :as node]))

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

(defn has-video-extension? [path] (node/match-extensions? path video-extensions))

(defn sample? [path] (boolean (re-find #"sample" path)))

(defn str-pre-case [s str]
  (or
   (first (filter #(= (.toLowerCase %) (.toLowerCase str)) s))
   str))

(defn with-target [{:keys [name season path] :as info} target]
  (let [target-part (str name "/Season " season "/" (node/basename path))]
    (merge info {:target (str target "/" target-part)
                 :target-part target-part})))

(defn show-lookup
  ([path] (show-lookup path []))
  ([path names]
     (->> (node/scandir path)
          (r/remove sample?)
          (r/filter has-video-extension?)
          (r/filter node/is-file?)
          (r/keep episode-info)
          (r/map #(update-in % [:name] (partial str-pre-case names))))))

(defn directory-picker [cursor owner]
  (reify
    om/IDidMount
    (did-mount [_]
      (-> owner .-refs .-input .getDOMNode (.setAttribute "nwdirectory" "nwdirectory")))

    om/IRenderState
    (render-state [_ {k :path}]
      (dom/div nil
        (dom/div #js {:style #js {:cursor "pointer"}
                      :className "mh-20"
                      :onClick #(-> owner .-refs .-input .getDOMNode .click)}
                 (get cursor k "Pick a directory"))
        (dom/input #js {:type "file"
                        :ref "input"
                        :style #js {:display "none"}
                        :onChange #(om/update! cursor k (event->value %))})))))

(defn scan [{:keys [source target]} cursor]
  (let [{:keys [searching]} @cursor]
    (when-not searching
      (om/update! cursor [:searching] true)
      (om/update! cursor [:matched] [])
      (go
        (try
          (let [names (<? (node/read-dir target))]
            (<! (dochan [info (show-lookup source names)]
                  (om/transact! cursor [:matched] #(conj % (with-target info target))))))
          (catch js/Error e
            (.log js/console "Error" e)))
        (om/update! cursor [:searching] false)))))

(defn scan-result [{:keys [path target target-part]}]
  (dom/div nil
    (dom/div nil target-part)
    (dom/div nil target)
    (dom/button #js {:onClick #(do (.preventDefault %)
                                   (gui/show-file path))
                     :href "#"}
                "show")))

(defn move [{:keys [path target] :as item}]
  (go-catch
    (let [d (node/dirname target)]
      (<? (node/mkdir-p d))
      (<? (node/rename path target))
      (>! flux-channel {:cmd :add-search
                        :path target})
      :done)))

(defn move-episodes [cursor]
  (go
    (>! flux-channel {:cmd :change-page :page :search})
    (<! (dochan [item (r/spool @cursor)]
           (try
             (<? (move item))
             (catch js/Error e
               (.log js/console "Error moving" (clj->js item) "err" e)))))
    (om/update! cursor [])))

(defn render-organize [cursor]
  (let [settings (-> cursor :settings :organizer)
        {:keys [searching matched] :as organizer} (-> cursor :organizer)
        run-scan #(scan (-> @cursor :settings :organizer) organizer)]
    (dom/div #js {:className "flex auto-scroll"}
      (dom/div #js {:className "white-box wb-top-detail"}
        "Video Organizer"
        (dom/div nil (dom/strong nil "Source"))
        (om/build directory-picker settings {:state {:path :source}})
        (dom/div nil (dom/strong nil "Target"))
        (om/build directory-picker settings {:state {:path :target}})
        (dom/button #js {:onClick run-scan
                         :disabled searching} "Scan"))
      (apply dom/div #js {:className "white-box"}
        (if searching
          "Searching...")
        (if (and (not searching)
                 (seq matched))
          (dom/button #js {:onClick #(move-episodes matched)} "MOVE!"))
        (map scan-result matched)))))
