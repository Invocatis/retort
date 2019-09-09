(ns retort.util)

(defn remove-keys
  [m ks]
  (let [m! (transient m)]
    (loop [ks ks]
      (if (empty? ks)
        (persistent! m!)
        (do
          (dissoc! m! (first ks))
          (recur (rest ks)))))))

(def ^:private html-tags
  #{:a
    :abbr
    :address
    :area
    :article
    :aside
    :audio
    :b
    :base
    :bdi
    :bdo
    :blockquote
    :body
    :br
    :button
    :canvas
    :caption
    :cite
    :code
    :col
    :colgroup
    :command
    :datalist
    :dd
    :del
    :details
    :dfn
    :div
    :dl
    :dt
    :em
    :embed
    :fieldset
    :figcaption
    :figure
    :footer
    :form
    :h1
    :h2
    :h3
    :h4
    :h5
    :h6
    :head
    :header
    :hgroup
    :hr
    :html
    :i
    :iframe
    :img
    :input
    :ins
    :kbd
    :keygen
    :label
    :legend
    :li
    :link
    :map
    :mark
    :math
    :menu
    :meta
    :meter
    :nav
    :noscript
    :object
    :ol
    :optgroup
    :option
    :output
    :p
    :param
    :pre
    :progress
    :q
    :rp
    :rt
    :ruby
    :s
    :samp
    :script
    :section
    :select
    :small
    :source
    :span
    :strong
    :style
    :sub
    :summary
    :sup
    :svg
    :table
    :tbody
    :td
    :textarea
    :tfoot
    :th
    :thead
    :time
    :title
    :tr
    :track
    :u
    :ul
    :var
    :video
    :wbr})

(defn html-tag?
  [any]
  (html-tags any))

(defn html-selector?
  [any]
  (not (and (re-matches #"[a-zA-Z0-9\-]+" (name any))
            (not (contains? html-tags any)))))
