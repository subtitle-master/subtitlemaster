(ns smgui.settings
  (:require [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true]
            [smgui.uuid :as uuid]))

(def defaults
  {:languages ["pb" "pt" "en"]
   :uuid (uuid/v4)})

(defn save [new-settings]
  (set! (-> js/window .-localStorage .-settings) (pr-str new-settings))
  new-settings)

(defn read []
  (let [settings (-> js/window .-localStorage .-settings)]
    (if settings
      (cljs.reader/read-string settings)
      (save defaults))))

(defn languages [] (-> (read) :languages))
(defn uuid [] (-> (read) :uuid))

(defn- extract-language [lang]
  {:locale (.-locale lang)
   :iso639_1 (.-iso639_1 lang)
   :iso639_2b (.-iso639_2b lang)
   :name (.-name lang)})

(.require js/window "subtitle-master") ; this ensures CoffeeScript is activated
(def languages-map (->> (.require js/window "subtitle-master/lib/languages.coffee")
                    (array-seq)
                    (map extract-language)
                    (sort-by :name)))

(defn- option-from-location [{:keys [iso639_1 name]}]
  (dom/option #js {:value iso639_1 :key iso639_1} name))

(defn language-from-iso639_1 [iso639_1]
  (first (filter #(= iso639_1 (:iso639_1 %)) languages-map)))

(defn in?
  [seq elm]
  (some #(= elm %) seq))

(defn add-items [cursor values]
  (om/transact! cursor #(vec (concat % values))))

(defn remove-all [seq values]
  (remove (partial in? values) seq))

(defn remove-items [cursor values]
  (om/transact! cursor #(vec (remove-all % values))))

(defn get-ref-values [owner ref]
  (smgui.components/input-value-seq (om/get-node owner ref)))

(defn index-of [s, needle]
  (->> (map-indexed vector s)
       (some (fn [[k v]] (and (= needle v)
                            k)))))

(defn- swap-indexes [seq a b]
  (-> seq
      (assoc b (nth seq a))
      (assoc a (nth seq b))))

(defn move-up-indexes [input indexes]
  (reduce
    (fn [list index]
      (let [next (dec index)]
        (if (and (>= next 0))
          (swap-indexes list index next)
          list)))
    input
    indexes))

(defn move-down-indexes [input indexes]
  (reduce
    (fn [list index]
      (let [next (inc index)]
        (if (and (< next (count list)))
          (swap-indexes list index next)
          list)))
    input
    (reverse indexes)))

(defn move-up [cursor selected]
  (let [indexes (map #(index-of @cursor %) selected)]
    (om/transact! cursor #(move-up-indexes % indexes))))

(defn move-down [cursor selected]
  (let [indexes (map  #(index-of @cursor %) selected)]
    (om/transact! cursor #(move-down-indexes % indexes))))

(defn language-picker [cursor owner]
  (reify
    om/IRender
    (render [this]
      (dom/table #js {:className "multiselect"}
        (dom/tbody nil
          (dom/tr nil
            (dom/td nil)
            (dom/td nil "Idiomas Selecionados")
            (dom/td nil)
            (dom/td nil "Idiomas Disponíveis"))
          (dom/tr nil
            (dom/td nil
              (dom/div #js {:className "flex-column flex-center"}
                (dom/button #js {:type "button"
                                 :onClick #(move-up cursor (get-ref-values owner "selected"))}
                            "↑")

                (dom/button #js {:type "button"
                                 :onClick #(move-down cursor (get-ref-values owner "selected"))}
                            "↓")))
            (dom/td nil
              (apply dom/select #js {:ref "selected"
                                     :size "20"
                                     :multiple "multiple"}
                     (->> cursor
                          (map language-from-iso639_1)
                          (map option-from-location))))
            (dom/td nil
              (dom/div #js {:className "flex-column flex-center"}
                       (dom/button #js {:type "button"
                                        :onClick #(add-items cursor (get-ref-values owner "available"))}
                                   "←")

                       (dom/button #js {:type "button"
                                        :onClick #(remove-items cursor (get-ref-values owner "selected"))}
                                   "→")))
            (dom/td nil
              (apply dom/select #js {:ref "available"
                                     :size "20"
                                     :multiple "multiple"}
                     (->> languages-map
                          (remove #(in? cursor (:iso639_1 %)))
                          (map option-from-location))))))))))

(defn render-page [cursor]
  (dom/div #js {:className "flex auto-scroll"}
           (dom/div #js {:className "white-box auto-scroll"} (om/build language-picker (get-in cursor [:settings :languages])))))
