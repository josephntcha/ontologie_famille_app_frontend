import { Component, computed, forwardRef, inject, input, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

import { PersonnesStore } from '../core/personnes.store';
import { Sexe } from '../core/models';

/**
 * Liste déroulante des personnes, utilisable avec [formControl] ou formControlName.
 * La valeur est l'id (number) ou null.
 */
@Component({
  selector: 'app-personne-select',
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => PersonneSelect), multi: true }],
  template: `
    <select
      [id]="inputId()"
      [disabled]="desactive()"
      [class.invalide]="invalide()"
      (change)="choisir($any($event.target).value)"
      (blur)="onTouched()"
    >
      <option value="" [selected]="valeur() === null">{{ placeholder() }}</option>
      @for (p of options(); track p.id) {
        <option [value]="p.id" [selected]="p.id === valeur()">
          {{ p.prenom }} {{ p.nom }} ({{ p.sexe === 'M' ? 'né' : 'née' }} en {{ p.date_naissance.slice(0, 4) }}) – n° {{ p.id }}
        </option>
      }
    </select>
  `,
  styles: `
    :host { display: block; }
  `,
})
export class PersonneSelect implements ControlValueAccessor {
  private readonly store = inject(PersonnesStore);

  readonly inputId = input<string>('');
  readonly placeholder = input('Choisir une personne');
  readonly exclure = input<number | null>(null);
  readonly sexe = input<Sexe | null>(null);
  readonly invalide = input(false);

  protected readonly valeur = signal<number | null>(null);
  protected readonly desactive = signal(false);

  protected readonly options = computed(() =>
    this.store
      .triees()
      .filter((p) => p.id !== this.exclure() || p.id === this.valeur())
      .filter((p) => !this.sexe() || p.sexe === this.sexe()),
  );

  private onChange: (v: number | null) => void = () => {};
  protected onTouched: () => void = () => {};

  protected choisir(v: string): void {
    const id = v === '' ? null : Number(v);
    this.valeur.set(id);
    this.onChange(id);
  }

  writeValue(v: number | null): void {
    this.valeur.set(v ?? null);
  }
  registerOnChange(fn: (v: number | null) => void): void {
    this.onChange = fn;
  }
  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }
  setDisabledState(d: boolean): void {
    this.desactive.set(d);
  }
}
