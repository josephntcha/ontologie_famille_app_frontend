package bj.ifri.parente.ontologie;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bj.ifri.parente.ontologie.AboxAssertee.Donnee;
import bj.ifri.parente.ontologie.AboxAssertee.Individu;
import bj.ifri.parente.ontologie.AboxAssertee.Lien;
import bj.ifri.parente.service.ChargeurGraphe;

/**
 * Export Turtle de l'ABox <b>assertée</b> (sans inférences) : même projection que celle donnée à
 * HermiT, sérialisée selon SPEC_API.md section 9. Les propriétés utilisées sont déclarées pour
 * que le document soit relu comme une ontologie OWL (termes tous présents dans la TBox).
 */
@Service
@Transactional(readOnly = true)
public class TurtleExporter {

    private final OntologieService ontologie;
    private final ChargeurGraphe chargeur;
    private final ProjectionABox projection;

    public TurtleExporter(OntologieService ontologie, ChargeurGraphe chargeur, ProjectionABox projection) {
        this.ontologie = ontologie;
        this.chargeur = chargeur;
        this.projection = projection;
    }

    public String exporter() {
        AboxAssertee abox = projection.projeter(chargeur.charger());
        StringBuilder sb = new StringBuilder();
        sb.append("@prefix f: <").append(ontologie.namespace()).append("> .\n");
        sb.append("@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n");
        sb.append("@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n");
        sb.append("@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n");
        sb.append("@prefix owl: <http://www.w3.org/2002/07/owl#> .\n\n");

        TreeSet<String> proprietesObjet = new TreeSet<>();
        TreeSet<String> proprietesDonnees = new TreeSet<>();
        for (Individu i : abox.individus()) {
            i.liens().forEach(l -> proprietesObjet.add(l.propriete()));
            i.donnees().forEach(d -> proprietesDonnees.add(d.propriete()));
        }
        if (!proprietesObjet.isEmpty() || !proprietesDonnees.isEmpty()) {
            sb.append("# Propriétés utilisées (déclarées dans la TBox famille.owl)\n");
            proprietesObjet.forEach(p -> sb.append("f:").append(p).append(" a owl:ObjectProperty .\n"));
            proprietesDonnees.forEach(p -> sb.append("f:").append(p).append(" a owl:DatatypeProperty .\n"));
            sb.append('\n');
        }

        for (Individu i : abox.individus()) {
            sb.append("f:").append(i.nom()).append(" a owl:NamedIndividual");
            i.classes().forEach(c -> sb.append(", f:").append(c));
            for (Donnee d : i.donnees()) {
                sb.append(" ;\n    f:").append(d.propriete()).append(' ').append(litteral(d));
            }
            Map<String, List<String>> parPropriete = new LinkedHashMap<>();
            for (Lien l : i.liens()) {
                parPropriete.computeIfAbsent(l.propriete(), k -> new ArrayList<>()).add("f:" + l.cible());
            }
            parPropriete.forEach((p, cibles) -> sb.append(" ;\n    f:").append(p).append(' ').append(String.join(", ", cibles)));
            sb.append(" .\n\n");
        }
        return sb.toString();
    }

    private static String litteral(Donnee d) {
        String texte = '"' + echapper(d.valeur()) + '"';
        return switch (d.type()) {
            case STRING -> texte;
            case INT -> texte + "^^xsd:int";
            case DATE_TIME -> texte + "^^xsd:dateTime";
        };
    }

    /** Échappement des chaînes Turtle (antislash, guillemet, fins de ligne, tabulation). */
    static String echapper(String s) {
        StringBuilder r = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case '\\' -> r.append("\\\\");
                case '"' -> r.append("\\\"");
                case '\n' -> r.append("\\n");
                case '\r' -> r.append("\\r");
                case '\t' -> r.append("\\t");
                default -> r.append(c);
            }
        }
        return r.toString();
    }
}
