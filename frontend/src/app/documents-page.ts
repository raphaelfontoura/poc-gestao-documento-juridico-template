import { Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Api, DocumentRecord, Template } from './api';

@Component({
  selector: 'app-documents-page',
  imports: [RouterLink],
  template: `
    <section class="page-head">
      <div><p class="eyebrow">Acervo de demonstração</p><h1>Documentos</h1>
        <p class="intro">Escolha um modelo, edite a peça e confirme os metadados extraídos.</p></div>
      <span class="count">{{ documents().length }} registros</span>
    </section>
    @if (error()) { <p class="notice error" role="alert">{{ error() }}</p> }
    <section class="section-block" aria-labelledby="new-title">
      <div class="section-heading"><div><p class="eyebrow">01 / Iniciar</p><h2 id="new-title">Nova peça</h2></div>
        <p>Escolha a origem da edição.</p></div>
      <div class="template-grid">
        @for (template of templates(); track template.id) {
          <article class="template-card">
            <div class="template-mark">{{ template.id === 'parecer' ? 'P' : 'PT' }}</div>
            <h3>{{ template.name }}</h3><p>{{ template.description }}</p>
            <p class="field-note">{{ template.fields.length }} campos de metadados</p>
            <div class="button-row">
              <button class="primary" [disabled]="busy()" (click)="create(template.id, true)">Criar no Google Docs</button>
              <button class="secondary" [disabled]="busy()" (click)="create(template.id, false)">Usar DOCX</button>
            </div>
          </article>
        }
      </div>
    </section>
    <section class="section-block" aria-labelledby="list-title">
      <div class="section-heading"><div><p class="eyebrow">02 / Consultar</p><h2 id="list-title">Peças registradas</h2></div></div>
      @if (loading()) { <p class="empty">Carregando documentos…</p> }
      @else if (!documents().length) { <p class="empty">Nenhuma peça ainda. Comece escolhendo um modelo acima.</p> }
      @else {
        <div class="document-list">
          @for (document of documents(); track document.id) {
            <a class="document-row" [routerLink]="['/documents', document.id]">
              <span class="document-icon">{{ document.templateId === 'parecer' ? 'P' : 'PT' }}</span>
              <span class="document-main"><strong>{{ label(document.templateId) }}</strong>
                <small>{{ document.id }}</small></span>
              <span class="badge" [class.done]="document.status === 'FINALIZADO'">{{ document.status === 'FINALIZADO' ? 'Finalizado' : 'Em edição' }}</span>
              <span class="row-arrow" aria-hidden="true">↗</span>
            </a>
          }
        </div>
      }
    </section>
  `,
})
export class DocumentsPage implements OnInit {
  private readonly api = inject(Api);
  private readonly router = inject(Router);
  readonly templates = signal<Template[]>([]);
  readonly documents = signal<DocumentRecord[]>([]);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly error = signal('');

  async ngOnInit(): Promise<void> {
    try {
      const [templates, documents] = await Promise.all([this.api.templates(), this.api.documents()]);
      this.templates.set(templates);
      this.documents.set(documents);
    } catch (error) { this.error.set(this.message(error)); }
    finally { this.loading.set(false); }
  }

  label(id: string): string { return this.templates().find(template => template.id === id)?.name ?? id; }

  async create(templateId: string, useDrive: boolean): Promise<void> {
    this.busy.set(true); this.error.set('');
    try {
      const document = await this.api.create(templateId, useDrive);
      await this.router.navigate(['/documents', document.id], { state: { driveError: document.driveError } });
    } catch (error) { this.error.set(this.message(error)); }
    finally { this.busy.set(false); }
  }

  private message(error: unknown): string { return error instanceof Error ? error.message : 'Não foi possível carregar os documentos.'; }
}
