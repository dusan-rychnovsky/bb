#!/usr/bin/env bb

; =============================================================================
; PHOTOS.CLJ
; USE: ./photos.clj --src [src-dir] --dest [dest-dir]
; * Will copy photos from src-dir to dest-dir, grouped by desired photo format.
; * Indicate desired format by prefixing photo filenames with:
; * - m for small,
; * - v for large.
; * Will notify and fail if you accidentally have multiple photos with the same
; * filename in src-dir.
; =============================================================================

(require '[clojure.tools.cli :refer [parse-opts]])
(use '[clojure.java.io :only (file, copy, make-parents)])

(defn parse-args [args]
  (let [options
          [["-s" "--src SOURCE-FOLDER" "Source folder"]
          ["-d" "--dest DESTINATION-FILDER" "Destination folder"]]
        args (:options (parse-opts args options))
       ]
    (do
      (assert (:src args) "--src must be specified")
      (assert (:dest args) "--dest must be specified")
      args)))

(def args (parse-args *command-line-args*))
    
(defn cathegory [name]
  (let [ch (first name)]  
    (cond
      (#{\m \M} ch) :small
      (#{\v \V} ch) :large
      :else (assert false (str "Unsupported cathegory: " ch)))))

(defn mark-seen [seen f]
  (do
    (assert (not (seen f)) (str "Duplicate file: " f))
    (conj seen f)))

(defn copy-file [seen f]
  (let [fname (.getName f)
        cat (cathegory fname)
        destf (file (str (:dest args) "/" (name cat) "/" fname))]
    (do
      (println cat (.getPath f))
      (make-parents destf)
      (copy f destf)
      (mark-seen seen fname))))

(def src-dir (file (:src args)))
(def src-files (filter #(.isFile %) (file-seq src-dir)))
(reduce copy-file #{} src-files)
