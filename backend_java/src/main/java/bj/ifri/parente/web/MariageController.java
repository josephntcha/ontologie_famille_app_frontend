package bj.ifri.parente.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import bj.ifri.parente.service.MariageService;
import bj.ifri.parente.web.dto.CreationMariage;
import bj.ifri.parente.web.dto.DissolutionMariage;
import bj.ifri.parente.web.dto.Donnees;
import bj.ifri.parente.web.dto.MariageDto;
import jakarta.validation.Valid;

/** Mariages (individus f:Mariage). */
@RestController
@RequestMapping("/api/mariages")
public class MariageController {

    private final MariageService service;

    public MariageController(MariageService service) {
        this.service = service;
    }

    @GetMapping
    public Donnees<List<MariageDto>> lister() {
        return new Donnees<>(service.lister());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Donnees<MariageDto> creer(@Valid @RequestBody CreationMariage corps) {
        return new Donnees<>(service.creer(corps));
    }

    @GetMapping("/{id}")
    public Donnees<MariageDto> afficher(@PathVariable Long id) {
        return new Donnees<>(service.afficher(id));
    }

    @PatchMapping("/{id}")
    public Donnees<MariageDto> dissoudre(@PathVariable Long id, @Valid @RequestBody DissolutionMariage corps) {
        return new Donnees<>(service.dissoudre(id, corps));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        service.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
