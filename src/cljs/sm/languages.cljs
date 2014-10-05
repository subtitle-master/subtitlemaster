(ns sm.languages)

(def languages
  [
    {:iso639_1 "sq" :iso639_2b "alb" :locale "sq"    :name "Albanian"}
    {:iso639_1 "ar" :iso639_2b "ara" :locale "ar"    :name "Arabic"}
    {:iso639_1 "hy" :iso639_2b "arm" :locale "hy"    :name "Armenian"}
    {:iso639_1 "ms" :iso639_2b "may" :locale "ms"    :name "Malay"}
    {:iso639_1 "bs" :iso639_2b "bos" :locale "bs"    :name "Bosnian"}
    {:iso639_1 "bg" :iso639_2b "bul" :locale "bg"    :name "Bulgarian"}
    {:iso639_1 "ca" :iso639_2b "cat" :locale "ca"    :name "Catalan"}
    {:iso639_1 "eu" :iso639_2b "eus" :locale "eu"    :name "Basque"}
    {:iso639_1 "zh" :iso639_2b "chi" :locale "zh_CN" :name "Chinese (China)"}
    {:iso639_1 "hr" :iso639_2b "hrv" :locale "hr"    :name "Croatian"}
    {:iso639_1 "cs" :iso639_2b "cze" :locale "cs"    :name "Czech"}
    {:iso639_1 "da" :iso639_2b "dan" :locale "da"    :name "Danish"}
    {:iso639_1 "nl" :iso639_2b "dut" :locale "nl"    :name "Dutch"}
    {:iso639_1 "en" :iso639_2b "eng" :locale "en"    :name "English"}
    {:iso639_1 "eo" :iso639_2b "epo" :locale "eo"    :name "Esperanto"}
    {:iso639_1 "et" :iso639_2b "est" :locale "et"    :name "Estonian"}
    {:iso639_1 "fi" :iso639_2b "fin" :locale "fi"    :name "Finnish"}
    {:iso639_1 "fr" :iso639_2b "fre" :locale "fr"    :name "French"}
    {:iso639_1 "gl" :iso639_2b "glg" :locale "gl"    :name "Galician"}
    {:iso639_1 "ka" :iso639_2b "geo" :locale "ka"    :name "Georgian"}
    {:iso639_1 "de" :iso639_2b "ger" :locale "de"    :name "German"}
    {:iso639_1 "el" :iso639_2b "ell" :locale "el"    :name "Greek"}
    {:iso639_1 "he" :iso639_2b "heb" :locale "he"    :name "Hebrew"}
    {:iso639_1 "hu" :iso639_2b "hun" :locale "hu"    :name "Hungarian"}
    {:iso639_1 "id" :iso639_2b "ind" :locale "id"    :name "Indonesian"}
    {:iso639_1 "it" :iso639_2b "ita" :locale "it"    :name "Italian"}
    {:iso639_1 "ja" :iso639_2b "jpn" :locale "ja"    :name "Japanese"}
    {:iso639_1 "kk" :iso639_2b "kaz" :locale "kk"    :name "Kazakh"}
    {:iso639_1 "ko" :iso639_2b "kor" :locale "ko"    :name "Korean"}
    {:iso639_1 "lv" :iso639_2b "lav" :locale "lv"    :name "Latvian"}
    {:iso639_1 "lt" :iso639_2b "lit" :locale "lt"    :name "Lithuanian"}
    {:iso639_1 "lb" :iso639_2b "ltz" :locale "lb"    :name "Luxembourgish"}
    {:iso639_1 "mk" :iso639_2b "mac" :locale "mk"    :name "Macedonian"}
    {:iso639_1 "no" :iso639_2b "nor" :locale "no"    :name "Norwegian"}
    {:iso639_1 "fa" :iso639_2b "per" :locale "fa"    :name "Persian"}
    {:iso639_1 "pl" :iso639_2b "pol" :locale "pl"    :name "Polish"}
    {:iso639_1 "pb" :iso639_2b "pob" :locale "pt_BR" :name "Portuguese (Brazilian)"}
    {:iso639_1 "pt" :iso639_2b "por" :locale "pt_PT" :name "Portuguese"}
    {:iso639_1 "ro" :iso639_2b "rum" :locale "ro"    :name "Romanian"}
    {:iso639_1 "ru" :iso639_2b "rus" :locale "ru"    :name "Russian"}
    {:iso639_1 "sr" :iso639_2b "scc" :locale "sr"    :name "Serbian"}
    {:iso639_1 "sk" :iso639_2b "slo" :locale "sk"    :name "Slovak"}
    {:iso639_1 "sl" :iso639_2b "slv" :locale "sl"    :name "Slovenian"}
    {:iso639_1 "es" :iso639_2b "spa" :locale "es_ES" :name "Spanish (Spain)"}
    {:iso639_1 "sv" :iso639_2b "swe" :locale "sv"    :name "Swedish"}
    {:iso639_1 "th" :iso639_2b "tha" :locale "th"    :name "Thai"}
    {:iso639_1 "tr" :iso639_2b "tur" :locale "tr"    :name "Turkish"}
    {:iso639_1 "uk" :iso639_2b "ukr" :locale "uk"    :name "Ukrainian"}
    {:iso639_1 "vi" :iso639_2b "vie" :locale "vi"    :name "Vietnamese"}])

(defn convert [lang in out]
  (out (first (filter #(= lang (in %)) languages))))

(defn iso-6391->iso639-2b [lang]
  (convert lang :iso639_1 :iso639_2b))
