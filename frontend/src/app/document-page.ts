import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Api, DocumentDetail, Preview, Template } from './api';

@Component({
  selector: 'app-document-page',
  imports: [RouterLink],
  template: `
    <a routerLink="/" class="back-link">← Todos os documentos</a>
    @if (loading()) { <p class="empty">Carregando documento…</p> }
    @else if (document(); as item) {
      <section class="page-head">
        <div><p class="eyebrow">{{ template()?.name ?? item.templateId }} · {{ item.id }}</p>
          <h1>{{ template()?.name ?? 'Documento' }}</h1>
          <p class="intro">{{ item.status === 'FINALIZADO' ? 'Metadados confirmados e arquivo preservado.' : 'Preencha o modelo e confira a prévia antes de finalizar.' }}</p></div>
        <span class="badge" [class.done]="item.status === 'FINALIZADO'">{{ item.status === 'FINALIZADO' ? 'Finalizado' : 'Em edição' }}</span>
      </section>
      @if (driveError()) { <p class="notice warning" role="status">{{ driveError() }} O upload de DOCX continua disponível.</p> }
      @if (error()) { <p class="notice error" role="alert">{{ error() }}</p> }
      @if (item.status === 'EM_EDICAO') {
        <div class="detail-grid">
          <section class="section-block work-panel">
            <p class="eyebrow">01 / Editar</p><h2>Documento em elaboração</h2>
            <p>Use a tabela <code>METADADOS_POC_V1</code> do modelo. Preserve as chaves e preencha somente os valores.</p>
            @if (item.editUrl) {
              <a class="primary link-button" [href]="item.editUrl" target="_blank" rel="noopener noreferrer">Abrir Google Docs ↗</a>
              <button class="secondary" [disabled]="busy()" (click)="previewDrive()">Ler versão atual do Drive</button>
            }
            <a class="secondary link-button" [href]="'/api/documents/' + item.id + '/template'">Baixar modelo DOCX ↓</a>
            <p class="hint">Sem Drive? Edite o DOCX, salve e envie abaixo.</p>
            <label class="upload-label" for="docx-upload">Enviar DOCX preenchido</label>
            <input id="docx-upload" type="file" accept=".docx,application/vnd.openxmlformats-officedocument.wordprocessingml.document" (change)="selectFile($event)" />
            <button class="primary" [disabled]="!selectedFile || busy()" (click)="upload()">Gerar prévia</button>
          </section>
          <section class="section-block preview-panel">
            <p class="eyebrow">02 / Conferir</p><h2>Prévia de metadados</h2>
            @if (preview(); as candidate) {
              <p class="notice" [class.error]="!candidate.valid" [class.success]="candidate.valid">{{ candidate.valid ? 'Pronto para confirmar.' : 'Corrija os campos indicados e envie uma nova versão.' }}</p>
              <dl class="metadata-list">
                @for (field of template()?.fields ?? []; track field.key) {
                  <div><dt>{{ field.label }}</dt><dd>{{ show(candidate.fields[field.key]) || '—' }}</dd>
                    @if (candidate.errors[field.key]) { <small class="field-error">{{ candidate.errors[field.key] }}</small> }</div>
                }
              </dl>
              @for (entry of structureErrors(candidate); track entry) { <p class="field-error">{{ entry }}</p> }
              <button class="primary confirm" [disabled]="!candidate.valid || busy()" (click)="confirm()">Confirmar finalização</button>
            } @else { <p class="empty">Envie um DOCX ou leia a versão atual do Drive para ver os campos aqui.</p> }
          </section>
        </div>
      } @else {
        <div class="detail-grid">
          <section class="section-block work-panel"><p class="eyebrow">Registro final</p><h2>Documento preservado</h2>
            <dl class="facts"><div><dt>Origem</dt><dd>{{ item.finalOrigin === 'DRIVE' ? 'Google Drive' : 'Upload DOCX' }}</dd></div>
              <div><dt>Finalizado em</dt><dd>{{ item.finalizedAt ? date(item.finalizedAt) : '—' }}</dd></div></dl>
            <a class="primary link-button" [href]="'/api/documents/' + item.id + '/file'">Baixar DOCX finalizado ↓</a>
          </section>
          <section class="section-block preview-panel"><p class="eyebrow">Metadados persistidos</p><h2>Dados capturados</h2>
            <dl class="metadata-list">@for (field of template()?.fields ?? []; track field.key) {
              <div><dt>{{ field.label }}</dt><dd>{{ show(item.metadata[field.key]) || '—' }}</dd></div>
            }</dl>
          </section>
        </div>
      }
    }
  `,
})
export class DocumentPage implements OnInit {
  private readonly api = inject(Api);
  private readonly route = inject(ActivatedRoute);
  readonly document = signal<DocumentDetail | null>(null);
  readonly template = signal<Template | null>(null);
  readonly preview = signal<Preview | null>(null);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly driveError = signal(history.state?.driveError ?? '');
  selectedFile: File | null = null;
  private readonly id = this.route.snapshot.paramMap.get('id') ?? '';

  async ngOnInit(): Promise<void> {
    try {
      const [document, templates] = await Promise.all([this.api.document(this.id), this.api.templates()]);
      this.document.set(document);
      this.template.set(templates.find(item => item.id === document.templateId) ?? null);
    } catch (error) { this.error.set(this.message(error)); }
    finally { this.loading.set(false); }
  }

  selectFile(event: Event): void { this.selectedFile = (event.target as HTMLInputElement).files?.[0] ?? null; }
  async upload(): Promise<void> {
    if (!this.selectedFile) return;
    await this.runPreview(() => this.api.upload(this.id, this.selectedFile!));
  }
  async previewDrive(): Promise<void> { await this.runPreview(() => this.api.previewFromDrive(this.id)); }
  private async runPreview(load: () => Promise<Preview>): Promise<void> {
    this.busy.set(true); this.error.set(''); this.preview.set(null);
    try { this.preview.set(await load()); }
    catch (error) { this.error.set(this.message(error)); }
    finally { this.busy.set(false); }
  }
  async confirm(): Promise<void> {
    const candidate = this.preview();
    if (!candidate?.valid) return;
    this.busy.set(true); this.error.set('');
    try { this.document.set(await this.api.confirm(this.id, candidate.id)); this.preview.set(null); }
    catch (error) { this.error.set(this.message(error)); }
    finally { this.busy.set(false); }
  }
  show(value: string | string[] | undefined): string { return Array.isArray(value) ? value.join(', ') : (value ?? ''); }
  structureErrors(preview: Preview): string[] { return Object.entries(preview.errors)
    .filter(([key]) => key === 'arquivo' || key === 'estrutura').map(([, value]) => value); }
  date(value: string): string { return new Date(value).toLocaleString('pt-BR'); }
  private message(error: unknown): string { return error instanceof Error ? error.message : 'Não foi possível carregar o documento.'; }
}
