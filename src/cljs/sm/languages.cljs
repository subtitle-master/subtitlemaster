(ns sm.languages)

(def languages
  [
    {:iso639_1 "sq" :iso639_2b "alb" :locale "sq" :name "Albanian" :name-br "Albanês"}
    {:iso639_1 "ar" :iso639_2b "ara" :locale "ar" :name "Arabic" :name-br "Arábico"}
    {:iso639_1 "hy" :iso639_2b "arm" :locale "hy" :name "Armenian" :name-br "Armênio"}
    {:iso639_1 "ms" :iso639_2b "may" :locale "ms" :name "Malay" :name-br "Malaio"}
    {:iso639_1 "bs" :iso639_2b "bos" :locale "bs" :name "Bosnian" :name-br "Bósnio"}
    {:iso639_1 "bg" :iso639_2b "bul" :locale "bg" :name "Bulgarian" :name-br "Búlgaro"}
    {:iso639_1 "ca" :iso639_2b "cat" :locale "ca" :name "Catalan" :name-br "Catalão"}
    {:iso639_1 "eu" :iso639_2b "eus" :locale "eu" :name "Basque" :name-br "Basque"}
    {:iso639_1 "zh" :iso639_2b "chi" :locale "zh_CN" :name "Chinese (China)" :name-br "Chinês"}
    {:iso639_1 "hr" :iso639_2b "hrv" :locale "hr" :name "Croatian" :name-br "Croata"}
    {:iso639_1 "cs" :iso639_2b "cze" :locale "cs" :name "Czech" :name-br "Czech"}
    {:iso639_1 "da" :iso639_2b "dan" :locale "da" :name "Danish" :name-br "Dinamarquês"}
    {:iso639_1 "nl" :iso639_2b "dut" :locale "nl" :name "Dutch" :name-br "Holandês"}
    {:iso639_1 "en" :iso639_2b "eng" :locale "en" :name "English" :name-br "Inglês"}
    {:iso639_1 "eo" :iso639_2b "epo" :locale "eo" :name "Esperanto" :name-br "Esperanto"}
    {:iso639_1 "et" :iso639_2b "est" :locale "et" :name "Estonian" :name-br "Estonian"}
    {:iso639_1 "fi" :iso639_2b "fin" :locale "fi" :name "Finnish" :name-br "Finlandês"}
    {:iso639_1 "fr" :iso639_2b "fre" :locale "fr" :name "French" :name-br "Francês"}
    {:iso639_1 "gl" :iso639_2b "glg" :locale "gl" :name "Galician" :name-br "Galego"}
    {:iso639_1 "ka" :iso639_2b "geo" :locale "ka" :name "Georgian" :name-br "Georgian"}
    {:iso639_1 "de" :iso639_2b "ger" :locale "de" :name "German" :name-br "Alemão"}
    {:iso639_1 "el" :iso639_2b "ell" :locale "el" :name "Greek" :name-br "Grego"}
    {:iso639_1 "he" :iso639_2b "heb" :locale "he" :name "Hebrew" :name-br "Hebraico"}
    {:iso639_1 "hu" :iso639_2b "hun" :locale "hu" :name "Hungarian" :name-br "Ungaro"}
    {:iso639_1 "id" :iso639_2b "ind" :locale "id" :name "Indonesian" :name-br "Indonesiano"}
    {:iso639_1 "it" :iso639_2b "ita" :locale "it" :name "Italian" :name-br "Italiano"}
    {:iso639_1 "ja" :iso639_2b "jpn" :locale "ja" :name "Japanese" :name-br "Japonês"}
    {:iso639_1 "kk" :iso639_2b "kaz" :locale "kk" :name "Kazakh" :name-br "Cazaquistão"}
    {:iso639_1 "ko" :iso639_2b "kor" :locale "ko" :name "Korean" :name-br "Koreano"}
    {:iso639_1 "lv" :iso639_2b "lav" :locale "lv" :name "Latvian" :name-br "Letônia"}
    {:iso639_1 "lt" :iso639_2b "lit" :locale "lt" :name "Lithuanian" :name-br "Lituânia"}
    {:iso639_1 "lb" :iso639_2b "ltz" :locale "lb" :name "Luxembourgish" :name-br "Luxemburguês"}
    {:iso639_1 "mk" :iso639_2b "mac" :locale "mk" :name "Macedonian" :name-br "Macedónio"}
    {:iso639_1 "no" :iso639_2b "nor" :locale "no" :name "Norwegian" :name-br "Norwegian"}
    {:iso639_1 "fa" :iso639_2b "per" :locale "fa" :name "Persian" :name-br "Persa"}
    {:iso639_1 "pl" :iso639_2b "pol" :locale "pl" :name "Polish" :name-br "Polonês"}
    {:iso639_1 "pb" :iso639_2b "pob" :locale "pt_BR" :name "Portuguese (Brazilian)" :name-br "Português (Brasil)"}
    {:iso639_1 "pt" :iso639_2b "por" :locale "pt_PT" :name "Portuguese" :name-br "Português (Portugual)"}
    {:iso639_1 "ro" :iso639_2b "rum" :locale "ro" :name "Romanian" :name-br "Romeno"}
    {:iso639_1 "ru" :iso639_2b "rus" :locale "ru" :name "Russian" :name-br "Russo"}
    {:iso639_1 "sr" :iso639_2b "scc" :locale "sr" :name "Serbian" :name-br "Sérvio"}
    {:iso639_1 "sk" :iso639_2b "slo" :locale "sk" :name "Slovak" :name-br "Eslovaco"}
    {:iso639_1 "sl" :iso639_2b "slv" :locale "sl" :name "Slovenian" :name-br "Esloveno"}
    {:iso639_1 "es" :iso639_2b "spa" :locale "es_ES" :name "Spanish (Spain)" :name-br "Espanhol"}
    {:iso639_1 "sv" :iso639_2b "swe" :locale "sv" :name "Swedish" :name-br "Sueco"}
    {:iso639_1 "th" :iso639_2b "tha" :locale "th" :name "Thai" :name-br "Thai"}
    {:iso639_1 "tr" :iso639_2b "tur" :locale "tr" :name "Turkish" :name-br "Turco"}
    {:iso639_1 "uk" :iso639_2b "ukr" :locale "uk" :name "Ukrainian" :name-br "Ucraniano"}
    {:iso639_1 "vi" :iso639_2b "vie" :locale "vi" :name "Vietnamese" :name-br "Vietnamita"}])

(defn convert [lang in out]
  (out (first (filter #(= lang (in %)) languages))))

(defn iso-6391->iso639-2b [lang]
  (convert lang :iso639_1 :iso639_2b))

(defn iso-6391->name-br [lang]
  (convert lang :iso639_1 :name-br))

(defn iso-iso639-2b->6391 [lang]
  (convert lang :iso639_2b :iso639_1))
