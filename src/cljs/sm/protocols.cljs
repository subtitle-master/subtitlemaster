(ns sm.protocols)

(defprotocol SearchProvider
  (search-subtitles [_ path languages]))

(defprotocol UploadProvider
  (upload-subtitle [_ path subtitle-path]))

(defprotocol Linkable
  (-linkable-url [_]))

(defprotocol Subtitle
  (download-stream [_])
  (subtitle-language [_]))

(defprotocol CacheStore
  (cache-exists? [_ key])
  (cache-store! [_ key]))

(defn upload-provider? [x] (satisfies? UploadProvider x))

(defn linkable? [x] (satisfies? Linkable x))
