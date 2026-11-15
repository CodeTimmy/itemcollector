# itemcollector
[Vibe Codet]

You can take a look at our [Roadmap](https://github.com/CodeTimmy/itemcollector/blob/main/Roadmap.md)

⚠️ Bekannte Einschränkungen (für spätere Verfeinerung)
Entitäts-Zuordnung: LivingDeathEvent weiß nicht direkt, welcher Spieler den Kill gemacht hat. Die aktuelle Lösung markiert für alle Spieler im 32-Block-Radius. Für präzisere Ergebnisse müsste man event.getSource().getEntity() auswerten und den ServerPlayer extrahieren.

Entity-Varianten: Schafe, Dorfbewohner usw. werden noch als eine einzige Entität behandelt. Phase 7 wird extraData nutzen, um Varianten zu unterscheiden.

Struktur-Erkennung: getStructureWithPieceAt mit einem always-true-Prädikat ist ineffizient bei vielen Strukturen. Eine spätere Optimierung könnte den Struktur-Tag des Spieler-Chunks cachen.
