import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { Api, Template } from './api';
import { DocumentsPage } from './documents-page';

const templates: Template[] = [
  { id: 'parecer', name: 'Parecer', description: 'Modelo de parecer', filename: 'parecer.docx', fields: [
    { key: 'assunto', label: 'Assunto', required: true, repeatable: false },
  ] },
  { id: 'portaria', name: 'Portaria', description: 'Modelo de portaria', filename: 'portaria.docx', fields: [
    { key: 'data', label: 'Data', required: true, repeatable: false },
  ] },
];

describe('DocumentsPage', () => {
  const api = {
    templates: vi.fn(),
    documents: vi.fn(),
    create: vi.fn(),
    downloadTemplate: vi.fn(),
  };

  beforeEach(async () => {
    api.templates.mockResolvedValue(templates);
    api.documents.mockResolvedValue([]);
    api.create.mockReset();
    api.downloadTemplate.mockReset();
    await TestBed.configureTestingModule({
      imports: [DocumentsPage],
      providers: [provideRouter([]), { provide: Api, useValue: api }],
    }).compileComponents();
  });

  it('mostra os tipos no combobox e o resumo da seleção', async () => {
    const fixture = TestBed.createComponent(DocumentsPage);
    fixture.detectChanges();
    await vi.waitFor(() => expect(fixture.componentInstance.loading()).toBe(false));
    fixture.detectChanges();
    const root = fixture.nativeElement as HTMLElement;
    const select = root.querySelector<HTMLSelectElement>('#manifestation-type')!;
    const buttons = Array.from(root.querySelectorAll<HTMLButtonElement>('.button-row button'));

    expect(Array.from(select.options).map(option => option.textContent?.trim())).toEqual([
      'Selecione um tipo', 'Parecer', 'Portaria',
    ]);
    expect(buttons.every(button => button.disabled)).toBe(true);

    select.value = 'parecer';
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    expect(root.querySelector('.template-summary')?.textContent).toContain('Assunto (obrigatório)');
    expect(buttons.every(button => !button.disabled)).toBe(true);

    select.value = 'portaria';
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    expect(root.querySelector('.template-summary')?.textContent).toContain('Data (obrigatório)');
    expect(root.querySelector('.template-summary')?.textContent).not.toContain('Assunto');
  });

  it('usa manifestação nos títulos e no estado vazio', async () => {
    const fixture = TestBed.createComponent(DocumentsPage);
    fixture.detectChanges();
    await vi.waitFor(() => expect(fixture.componentInstance.loading()).toBe(false));
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Nova manifestação');
    expect(text).toContain('Manifestações registradas');
    expect(text).toContain('Nenhuma manifestação ainda');
    expect(text).not.toMatch(/\bpeças?\b/i);
  });

  it('cria no Drive com o tipo selecionado e preserva a falha no registro', async () => {
    const fixture = TestBed.createComponent(DocumentsPage);
    fixture.detectChanges();
    await vi.waitFor(() => expect(fixture.componentInstance.loading()).toBe(false));
    fixture.detectChanges();
    const root = fixture.nativeElement as HTMLElement;
    const select = root.querySelector<HTMLSelectElement>('#manifestation-type')!;
    select.value = 'portaria';
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    api.create.mockResolvedValue({ id: 'registro-1', templateId: 'portaria', driveError: 'Drive indisponível' });
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    root.querySelector<HTMLButtonElement>('.button-row .primary')!.click();
    await vi.waitFor(() => expect(navigate).toHaveBeenCalled());

    expect(api.create).toHaveBeenCalledWith('portaria', true);
    expect(navigate).toHaveBeenCalledWith(['/documents', 'registro-1'], {
      state: { driveError: 'Drive indisponível' },
    });
  });

  it('baixa o modelo do registro criado e abre o detalhe para upload', async () => {
    const fixture = TestBed.createComponent(DocumentsPage);
    fixture.detectChanges();
    await vi.waitFor(() => expect(fixture.componentInstance.loading()).toBe(false));
    fixture.detectChanges();
    const root = fixture.nativeElement as HTMLElement;
    const select = root.querySelector<HTMLSelectElement>('#manifestation-type')!;
    select.value = 'parecer';
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    api.create.mockResolvedValue({ id: 'registro-parecer', templateId: 'parecer' });
    const file = new Blob(['docx']);
    api.downloadTemplate.mockResolvedValue(file);
    const createUrl = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:modelo');
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {});
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {});
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    root.querySelector<HTMLButtonElement>('.button-row .secondary')!.click();
    await vi.waitFor(() => expect(navigate).toHaveBeenCalled());

    expect(api.create).toHaveBeenCalledWith('parecer', false);
    expect(api.downloadTemplate).toHaveBeenCalledWith('registro-parecer');
    expect(createUrl).toHaveBeenCalledWith(file);
    expect((click.mock.instances[0] as HTMLAnchorElement).download).toBe('parecer.docx');
    expect(navigate).toHaveBeenCalledWith(['/documents', 'registro-parecer']);
    expect(root.querySelector('.button-row .secondary')?.textContent).toContain('Baixar modelo');
  });

  it('mantém a seleção quando a criação falha', async () => {
    const fixture = TestBed.createComponent(DocumentsPage);
    fixture.detectChanges();
    await vi.waitFor(() => expect(fixture.componentInstance.loading()).toBe(false));
    fixture.detectChanges();
    const root = fixture.nativeElement as HTMLElement;
    const select = root.querySelector<HTMLSelectElement>('#manifestation-type')!;
    select.value = 'portaria';
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    api.create.mockRejectedValue(new Error('Falha ao criar'));

    root.querySelector<HTMLButtonElement>('.button-row .secondary')!.click();
    await vi.waitFor(() => { fixture.detectChanges(); expect(root.querySelector('[role="alert"]')?.textContent).toContain('Falha ao criar'); });

    expect(select.value).toBe('portaria');
    expect(api.downloadTemplate).not.toHaveBeenCalled();
    expect(root.querySelector('a[href="/documents/"]')).toBeNull();
  });

  it('reusa o registro após falha de download e permite abri-lo', async () => {
    const fixture = TestBed.createComponent(DocumentsPage);
    fixture.detectChanges();
    await vi.waitFor(() => expect(fixture.componentInstance.loading()).toBe(false));
    fixture.detectChanges();
    const root = fixture.nativeElement as HTMLElement;
    const select = root.querySelector<HTMLSelectElement>('#manifestation-type')!;
    select.value = 'parecer';
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    api.create.mockResolvedValue({ id: 'registro-2', templateId: 'parecer' });
    api.downloadTemplate.mockRejectedValueOnce(new Error('Falha ao baixar'));

    const button = root.querySelector<HTMLButtonElement>('.button-row .secondary')!;
    button.click();
    await vi.waitFor(() => { fixture.detectChanges(); expect(root.querySelector('[role="alert"]')?.textContent).toContain('Falha ao baixar'); });
    expect(select.value).toBe('parecer');
    expect(root.querySelector<HTMLAnchorElement>('a[href="/documents/registro-2"]')?.textContent).toContain('abrir o registro');

    api.downloadTemplate.mockResolvedValue(new Blob(['docx']));
    vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:modelo');
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {});
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {});
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    button.click();
    await vi.waitFor(() => expect(navigate).toHaveBeenCalledWith(['/documents', 'registro-2']));

    expect(api.create).toHaveBeenCalledTimes(1);
    expect(api.downloadTemplate).toHaveBeenCalledTimes(2);
  });
});
