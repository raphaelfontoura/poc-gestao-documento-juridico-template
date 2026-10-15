"""Generate the two editable, fictional DOCX templates without external packages."""
from pathlib import Path
from xml.sax.saxutils import escape
from zipfile import ZipFile, ZIP_DEFLATED

ROOT = Path(__file__).resolve().parents[1] / "backend/src/main/resources/templates"
ROOT.mkdir(parents=True, exist_ok=True)

CONTENT_TYPES = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>'''
RELATIONSHIPS = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>'''


def paragraph(value: str, bold: bool = False) -> str:
    formatting = "<w:rPr><w:b/></w:rPr>" if bold else ""
    return f'<w:p><w:r>{formatting}<w:t xml:space="preserve">{escape(value)}</w:t></w:r></w:p>'


def cell(value: str) -> str:
    return f'<w:tc><w:tcPr><w:tcW w:w="4500" w:type="dxa"/></w:tcPr>{paragraph(value)}</w:tc>'


def table(fields: list[tuple[str, str]]) -> str:
    rows = [("Chave", "Valor"), *fields]
    body = "".join(f'<w:tr>{cell(key)}{cell(value)}</w:tr>' for key, value in rows)
    borders = '<w:tblBorders>' + ''.join(
        f'<w:{side} w:val="single" w:sz="4"/>'
        for side in ("top", "left", "bottom", "right", "insideH", "insideV")
    ) + '</w:tblBorders>'
    return f'<w:tbl><w:tblPr>{borders}</w:tblPr><w:tblGrid><w:gridCol w:w="4500"/><w:gridCol w:w="4500"/></w:tblGrid>{body}</w:tbl>'


def make(filename: str, title: str, fields: list[tuple[str, str]], body: list[str]) -> None:
    instructions = (
        "Preencha a segunda coluna. Use uma pessoa por linha nos campos de pessoas e "
        "data no formato dd/MM/aaaa. Preserve o marcador e as chaves da primeira coluna."
    )
    content = [paragraph(title, True), paragraph("POC — conteúdo fictício, sem orientação jurídica."),
               paragraph(instructions), paragraph("METADADOS_POC_V1", True), table(fields)]
    content.extend(paragraph(text) for text in body)
    document = ('<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
                '<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">'
                '<w:body>' + ''.join(content) + '<w:sectPr/></w:body></w:document>')
    with ZipFile(ROOT / filename, 'w', ZIP_DEFLATED) as docx:
        docx.writestr('[Content_Types].xml', CONTENT_TYPES)
        docx.writestr('_rels/.rels', RELATIONSHIPS)
        docx.writestr('word/document.xml', document)


make('parecer.docx', 'PARECER Nº [número]/[ano]', [
    ('interessados', '[Uma pessoa por linha]'),
    ('assunto', '[Assunto]'),
    ('ementa', '[Resumo do parecer]'),
    ('data', '[dd/MM/aaaa]'),
    ('procuradores_assinantes', '[Uma pessoa por linha]'),
], [
    'I — Relatório',
    '[Descreva a consulta e os fatos relevantes.]',
    'II — Análise',
    '[Desenvolva a fundamentação.]',
    'III — Conclusão',
    '[Registre a conclusão.]',
])

make('portaria.docx', 'PORTARIA Nº [número]/[ano]', [
    ('assunto', '[Assunto]'),
    ('data', '[dd/MM/aaaa]'),
    ('signatarios', '[Uma pessoa por linha]'),
], [
    '[Identifique a autoridade e o fundamento.]',
    'RESOLVE:',
    'Art. 1º [Descreva a providência.]',
    'Art. 2º [Indique a vigência.]',
    '[Local e assinatura.]',
])
