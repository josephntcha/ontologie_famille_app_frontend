package bj.ifri.parente.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import bj.ifri.parente.service.ArbreService;
import bj.ifri.parente.service.VerificationService;
import bj.ifri.parente.web.dto.ArbreDto;
import bj.ifri.parente.web.dto.DemandeVerification;
import bj.ifri.parente.web.dto.VerificationDto;
import jakarta.validation.Valid;

/** Écrans « Vérification » et « Arbre ». */
@RestController
@RequestMapping("/api")
public class VerificationController {

    private final VerificationService verification;
    private final ArbreService arbre;

    public VerificationController(VerificationService verification, ArbreService arbre) {
        this.verification = verification;
        this.arbre = arbre;
    }

    @PostMapping("/verification")
    public VerificationDto verifier(@Valid @RequestBody DemandeVerification corps) {
        return verification.verifier(corps.personne1Id(), corps.personne2Id());
    }

    @GetMapping("/arbre")
    public ArbreDto arbre(@RequestParam(name = "personne1_id", required = false) String personne1,
            @RequestParam(name = "personne2_id", required = false) String personne2) {
        return arbre.construire(personne1, personne2);
    }
}
