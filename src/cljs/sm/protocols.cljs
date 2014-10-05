(ns sm.protocols)

(defprotocol SearchProvider
  (search-subtitles [_ path languages]))

(defprotocol Subtitle
  (download-stream [_])
  (subtitle-language [_]))
