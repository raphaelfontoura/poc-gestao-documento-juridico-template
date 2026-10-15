import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Api, DocumentRecord, Template } from './api';

@Component({
  selector: 'app-documents-page',
  imports: [RouterLink],
  template: `
    <section class="page-head">
      <div><p class="eyebrow">Acervo de demonstração</p><h1>Documentos</h1>
        <p class="intro">Escolha um tipo de manifestação, edite o modelo e confirme os metadados extraídos.</p></div>
      <span class="count">{{ documents().length }} registros</span>
    </section>
    @if (error()) { <p class="notice error" role="alert">{{ error() }}</p> }
    @if (pendingDownload(); as pending) {
      <p class="notice warning" role="status">A manifestação já foi criada. Você pode repetir o download ou
        <a [routerLink]="['/documents', pending.id]">abrir o registro</a> para baixar o modelo e enviar o DOCX preenchido.</p>
    }
    <section class="section-block" aria-labelledby="new-title">
      <div class="section-heading"><div><p class="eyebrow">01 / Iniciar</p><h2 id="new-title">Nova manifestação</h2></div>
        <p>Escolha a origem da edição.</p></div>
      <div class="template-card">
        <label for="manifestation-type">Tipo de manifestação</label>
        <select id="manifestation-type" [value]="selectedTemplateId()" [disabled]="loading() || busy() || !templates().length" (change)="selectTemplate($event)">
          <option value="">Selecione um tipo</option>
          @for (template of templates(); track template.id) {
            <option [value]="template.id">{{ template.name }}</option>
          }
        </select>
        @if (selectedTemplate(); as template) {
          <div class="template-summary">
            <h3>{{ template.name }}</h3>
            <p>{{ template.description }}</p>
            <p class="field-note">Campos de metadados:</p>
            <ul>
              @for (field of template.fields; track field.key) {
                <li>{{ field.label }}{{ field.required ? ' (obrigatório)' : '' }}</li>
              }
            </ul>
          </div>
        }
        <div class="button-row">
          <button class="primary" [disabled]="loading() || busy() || !selectedTemplate()" (click)="createInDrive()">Criar no Google Docs</button>
          <button class="secondary" [disabled]="loading() || busy() || !selectedTemplate()" (click)="downloadModel()">Baixar modelo</button>
        </div>
      </div>
    </section>
    <section class="section-block" aria-labelledby="list-title">
      <div class="section-heading"><div><p class="eyebrow">02 / Consultar</p><h2 id="list-title">Manifestações registradas</h2></div></div>
      @if (loading()) { <p class="empty">Carregando documentos…</p> }
      @else if (!documents().length) { <p class="empty">Nenhuma manifestação ainda. Comece escolhendo um tipo acima.</p> }
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
  readonly selectedTemplateId = signal('');
  readonly selectedTemplate = computed(() => this.templates().find(template => template.id === this.selectedTemplateId()));
  readonly documents = signal<DocumentRecord[]>([]);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly pendingDownload = signal<{ id: string; templateId: string } | null>(null);

  async ngOnInit(): Promise<void> {
    try {
      const [templates, documents] = await Promise.all([this.api.templates(), this.api.documents()]);
      this.templates.set(templates);
      this.documents.set(documents);
    } catch (error) { this.error.set(this.message(error)); }
    finally { this.loading.set(false); }
  }

  label(id: string): string { return this.templates().find(template => template.id === id)?.name ?? id; }

  selectTemplate(event: Event): void { this.selectedTemplateId.set((event.target as HTMLSelectElement).value); }

  async createInDrive(): Promise<void> {
    const template = this.selectedTemplate();
    if (!template || this.busy()) return;
    await this.create(template.id, true);
  }

  async downloadModel(): Promise<void> {
    const template = this.selectedTemplate();
    if (!template || this.busy()) return;
    this.busy.set(true); this.error.set('');
    try {
      let recordId = this.pendingDownload()?.templateId === template.id ? this.pendingDownload()!.id : null;
      if (!recordId) {
        const record = await this.api.create(template.id, false);
        recordId = record.id;
        this.pendingDownload.set({ id: recordId, templateId: template.id });
      }
      const file = await this.api.downloadTemplate(recordId);
      const url = URL.createObjectURL(file);
      try {
        const link = document.createElement('a');
        link.href = url;
        link.download = template.filename;
        document.body.append(link);
        link.click();
        link.remove();
        await this.router.navigate(['/documents', recordId]);
        this.pendingDownload.set(null);
      } finally { setTimeout(() => URL.revokeObjectURL(url), 1000); }
    } catch (error) { this.error.set(this.message(error)); }
    finally { this.busy.set(false); }
  }

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
