import { Injectable } from '@angular/core';

export interface FieldDefinition { key: string; label: string; required: boolean; repeatable: boolean; }
export interface Template { id: string; name: string; description: string; filename: string; fields: FieldDefinition[]; }
export interface DocumentRecord {
  id: string; templateId: string; status: 'EM_EDICAO' | 'FINALIZADO';
  driveFileId: string | null; editUrl: string | null; finalOrigin: 'UPLOAD' | 'DRIVE' | null;
  createdAt: string; finalizedAt: string | null;
}
export interface DocumentDetail extends DocumentRecord { metadata: Record<string, string | string[]>; }
export interface CreatedDocument {
  id: string; templateId: string; status: 'EM_EDICAO';
  driveFileId: string | null; editUrl: string | null; driveError: string | null;
}
export interface Preview {
  id: string; documentId: string; origin: 'UPLOAD' | 'DRIVE'; valid: boolean;
  fields: Record<string, string | string[]>; errors: Record<string, string>;
}

@Injectable({ providedIn: 'root' })
export class Api {
  private async request<T>(path: string, options?: RequestInit): Promise<T> {
    const response = await fetch(`/api${path}`, options);
    if (!response.ok) {
      const body = await response.json().catch(() => ({}));
      throw new Error(body.error || `Falha na solicitação (HTTP ${response.status}).`);
    }
    return response.json() as Promise<T>;
  }

  templates(): Promise<Template[]> { return this.request('/templates'); }
  documents(): Promise<DocumentRecord[]> { return this.request('/documents'); }
  document(id: string): Promise<DocumentDetail> { return this.request(`/documents/${id}`); }
  create(templateId: string, useDrive: boolean): Promise<CreatedDocument> {
    return this.request('/documents', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ templateId, useDrive }),
    });
  }
  async downloadTemplate(id: string): Promise<Blob> {
    const response = await fetch(`/api/documents/${id}/template`);
    if (!response.ok) throw new Error(`Não foi possível baixar o modelo (HTTP ${response.status}).`);
    return response.blob();
  }
  upload(id: string, file: File): Promise<Preview> {
    const form = new FormData();
    form.append('file', file);
    return this.request(`/documents/${id}/previews/upload`, { method: 'POST', body: form });
  }
  previewFromDrive(id: string): Promise<Preview> {
    return this.request(`/documents/${id}/previews/drive`, { method: 'POST' });
  }
  confirm(id: string, previewId: string): Promise<DocumentDetail> {
    return this.request(`/documents/${id}/previews/${previewId}/confirm`, { method: 'POST' });
  }
}
