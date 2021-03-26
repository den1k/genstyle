(ns genstyle.web.landing)

(defn view []
  [:main.landing
   [:section.main-section
    [:div.main-headers
     [:h1.header "Genstyle"]
     [:h3.subheader "WHO NEEDS DESIGNERS?!?! 🙅‍♂️"]
     [:h6.premise
      "Genstyle simulates evolution by creating a population of organism-like CSS
      styles that compete with each other for the right to reproduce."]]]
   [:section.endorsement-section
    [:div.endorsement
     [:q.quote "I was a totally normal kid just going about my
    totally normal life. But then, one day, out of nowhere really,
     I watched an Apple Keynote where Steve Jobs said
      \"A stylus? Who wants a stylus?\" I pondered his message
      for a while. Who uses styluses? Of course! Designers! Steve
      was known to be sneaky but I saw right through it.
      It was a diss against designers. He envied Microsoft's
      spartan, low-taste aesthetic. But Apple never managed to
      rid itself from its designers. Until today..."]
     [:div.quote-author "— Dennis Heihoff (Genstyle Mastermind)"]]]
   [:section.pricing-section
    [:div.pricing-tiers
     [:h2.pricing-header "Pricing"]
     [:div.pricing-tier.free-tier
      [:h2.tier-name "Whatever!"]
      [:div "free"]]
     [:div.pricing-tier.startup-tier
      [:h2.tier-name "Startup"]
      [:div "not so free"]]
     [:div.pricing-tier.enterprise-tier
      [:h2.tier-name "Enterprise"]
      [:div "the talk to our sales team kind of not free"]]]]
   ])