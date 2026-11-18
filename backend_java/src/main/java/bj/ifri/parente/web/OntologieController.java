package bj.ifri.parente.web;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import bj.ifri.parente.ontologie.Coherence;
import bj.ifri.parente.ontologie.OntologieService;
import bj.ifri.parente.ontologie.RaisonneurService;
import bj.ifri.parente.ontologie.TurtleExporter;
import bj.ifri.parente.web.dto.CoherenceDto;
import bj.ifri.parente.web.dto.OntologieDto;

/** Ontologie (TBox), cohérence selon HermiT et export Turtle de l'ABox. */
@RestController
@RequestMapping("/api")
public class OntologieController {

    public static final MediaType TEXT_TURTLE = new MediaType("text", "turtle", java.nio.charset.StandardCharsets.UTF_8);

    private final OntologieService ontologie;
    private final RaisonneurService raisonneur;
    private final TurtleExporter exporter;

    public OntologieController(OntologieService ontologie, RaisonneurService raisonneur, TurtleExporter exporter) {
        this.ontologie = ontologie;
        this.raisonneur = raisonneur;
        this.exporter = exporter;
    }

    @GetMapping("/ontologie")
    public OntologieDto ontologie() {
        return new OntologieDto(ontologie.namespace(), ontologie.classes(), ontologie.proprietesObjet(),
                ontologie.proprietesDonnees());
    }

    @GetMapping("/raisonneur/coherence")
    public CoherenceDto coherence() {
        Coherence c = raisonneur.verifierCoherence();
        return new CoherenceDto(RaisonneurService.NOM, c.coherente(), c.classesInsatisfiables());
    }

    @GetMapping("/export/turtle")
    public ResponseEntity<String> turtle() {
        return ResponseEntity.ok().contentType(TEXT_TURTLE).body(exporter.exporter());
    }
}
