package bj.ifri.parente.service;

/** Niveau de complétude de l'ascendance d'une personne (SPEC_API.md 5.5). */
public record Completude(int parentsConnus, int parentsAttendus, int grandsParentsConnus, int grandsParentsAttendus,
        int generationsCompletes, boolean complet) {
}
